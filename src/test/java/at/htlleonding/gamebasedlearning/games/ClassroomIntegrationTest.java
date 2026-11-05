package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.users.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ClassroomIntegrationTest {
    @Inject GameRepository games;
    @Inject Smart10SnapshotLoader loader;
    @Inject Smart10SoloService solo;
    @Inject Smart10Tickets tickets;
    @Inject Smart10Feature feature;
    @Inject CompletedRoundStore store;
    @Inject ObjectMapper mapper;
    @Inject EntityManager em;
    @Inject UserService users;
    @Inject PublicGameView publicView;

    private AppUser user(UserRole role) {
        AppUser user = new AppUser();
        user.username="test"+UUID.randomUUID().toString().substring(0,8);
        user.externalSubject=UUID.randomUUID().toString();user.displayName=user.username;
        user.schoolClass="5TEST";user.role=role;user.active=true;
        QuarkusTransaction.requiringNew().run(()->em.persist(user));return user;
    }
    private Game game(String type, AppUser owner) {
        Game game=new Game();game.id=UUID.randomUUID().toString();game.name="Testspiel";game.gameType=type;
        Question question=new Question();question.text="Programmieren";question.type="JustOne".equals(type)?"JUST_ONE":"TRUE_FALSE";
        if ("JustOne".equals(type)) {
            for (int i=1;i<=5;i++) {AnswerOption option=new AnswerOption();option.text="Geheim"+i;question.answerOptions.add(option);}
        } else {
            question.smart10Cards=new ArrayList<>();
            for (int i=1;i<=12;i++) question.smart10Cards.add(new Smart10Card(null,i,"Aussage"+i,true));
        }
        game.questions.add(question);return games.create(game,owner);
    }
    private record Client(WebSocketConnection connection, List<String> messages) { }
    private Client client(AppUser user) {
        String id=UUID.randomUUID().toString();List<String> messages=new ArrayList<>();
        WebSocketConnection connection=(WebSocketConnection)Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{WebSocketConnection.class},(proxy,method,args)->switch(method.getName()) {
                    case "id" -> id;
                    case "isOpen" -> true;
                    case "sendTextAndAwait" -> {messages.add(args[0].toString());yield null;}
                    case "toString" -> id;
                    case "hashCode" -> id.hashCode();
                    case "equals" -> proxy==args[0];
                    default -> null;
                });
        tickets.bind(id,tickets.issue(user));return new Client(connection,messages);
    }
    private MultiplayerGameService engine() {
        MultiplayerGameService engine=new MultiplayerGameService();engine.games=games;engine.loader=loader;
        engine.tickets=tickets;engine.feature=feature;engine.store=store;engine.objectMapper=mapper;return engine;
    }
    private ObjectNode request(String code) {return mapper.createObjectNode().put("code",code);}
    private ObjectNode create(Game game,int teams, boolean hostPlays) {
        return mapper.createObjectNode().put("gameId",game.id).put("teamCount",teams).put("hostPlays",hostPlays).put("turnSeconds",5);
    }
    private void expire(MultiplayerGameService engine,String code,String field) throws Exception {
        Field rooms=MultiplayerGameService.class.getDeclaredField("rooms");rooms.setAccessible(true);
        Object room=((Map<?,?>)rooms.get(engine)).get(code);
        Field value=room.getClass().getDeclaredField(field);value.setAccessible(true);
        value.set(room,Instant.now().minusSeconds(field.equals("createdAt")?86401:1));
    }

    @Test void soloGradesPersistsAndRetriesWithPinnedVersion() {
        AppUser user=user(UserRole.STUDENT);Game game=game("Smart10",user);
        Smart10SoloService.Start start=solo.start(user.id,game.id);
        int score=0;
        for (var card:start.questions().getFirst().cards()) {
            var feedback=solo.answer(user.id,start.roundId(),card.id(),mapper.getNodeFactory().booleanNode(true));
            assertTrue(feedback.correct());score=feedback.score();
        }
        assertEquals(46,score);
        var first=solo.finish(user.id,start.roundId());
        assertEquals(first.sessionId(),solo.finish(user.id,start.roundId()).sessionId());
        assertEquals(46,store.totalPoints(user.id));
        assertThrows(IllegalArgumentException.class,()->solo.finish(UUID.randomUUID(),start.roundId()));
    }
    @Test void healthChecksDatabaseConnection() {
        io.restassured.RestAssured.given().get("/api/health").then().statusCode(200)
                .body("status",org.hamcrest.Matchers.equalTo("UP")).body("database",org.hamcrest.Matchers.equalTo("UP"));
    }
    @Test void justOneAttemptFailureRollsBackWholeResult() {
        AppUser host=user(UserRole.TEACHER),student=user(UserRole.STUDENT);
        Game game=game("JustOne",host);UUID key=UUID.randomUUID(),attemptId=UUID.randomUUID();
        var round=new CompletedRoundStore.Round(key,CompletedRoundStore.State.COMPLETED,game.id,game.currentVersionId,
                host.id,"MULTIPLAYER",Instant.now().minusSeconds(10),Instant.now(),
                List.of(new CompletedRoundStore.Team("Team 1",1,1)),
                List.of(new CompletedRoundStore.Participant(student.id,"Team 1",1,1,Instant.now().minusSeconds(5),List.of())));
        var attempt=new CompletedRoundStore.Attempt(attemptId,"Team 1",true,false);
        assertThrows(RuntimeException.class,()->store.saveJustOne(round,List.of(attempt,attempt)));
        assertEquals(0,QuarkusTransaction.requiringNew().call(()->((Number)em.createNativeQuery(
                "select count(*) from game_sessions where completion_key=?1").setParameter(1,key).getSingleResult()).intValue()));
        UUID session=store.saveJustOne(round,List.of(attempt));
        assertEquals(session,store.saveJustOne(round,List.of(attempt)));
    }

    @Test void allThreeModesUsePinnedVersionsAndRejectRepeatedAnswers() {
        for (String mode:List.of("TRUE_FALSE","FREE_TEXT","ORDERING")) {
            AppUser player=user(UserRole.STUDENT);Game game=game("Smart10",player);
            Question q=game.questions.getFirst();q.type=mode;q.smart10Cards=new ArrayList<>();
            for(int i=1;i<=12;i++)q.smart10Cards.add(new Smart10Card(null,i,"Karte"+i,
                    mode.equals("TRUE_FALSE")?true:mode.equals("FREE_TEXT")?"Antwort"+i:i));
            game=games.update(game.id,game).orElseThrow();
            Smart10SoloService.Start started=solo.start(player.id,game.id);
            Game changed=games.findById(game.id).orElseThrow();
            changed.questions.getFirst().text="Nach dem Start geändert";
            games.update(game.id,changed);
            for(int i=0;i<12;i++) {
                var card=started.questions().getFirst().cards().get(i);
                JsonNode answer=mode.equals("TRUE_FALSE")?mapper.getNodeFactory().booleanNode(true)
                        :mode.equals("FREE_TEXT")?mapper.getNodeFactory().textNode("antwort"+(i+1))
                        :mapper.getNodeFactory().numberNode(i+1);
                assertTrue(solo.answer(player.id,started.roundId(),card.id(),answer).correct());
                if(i==0)assertThrows(IllegalArgumentException.class,()->solo.answer(player.id,started.roundId(),card.id(),answer));
            }
            assertEquals(46,solo.finish(player.id,started.roundId()).score());
        }
    }

    @Test void statisticsOnlyTeachersAndAdminsSeeAllClassesWithoutAnswerContent() {
        AppUser student=user(UserRole.STUDENT), teacher=user(UserRole.TEACHER), other=user(UserRole.STUDENT);
        QuarkusTransaction.requiringNew().run(()->em.find(AppUser.class,other.id).schoolClass="4OTHER");
        Game game=game("Smart10",teacher);
        var round=solo.start(student.id,game.id);
        for(var card:round.questions().getFirst().cards())solo.answer(student.id,round.roundId(),card.id(),mapper.getNodeFactory().booleanNode(true));
        solo.finish(student.id,round.roundId());
        StatisticsResource resource=new StatisticsResource();resource.users=users;resource.em=em;
        resource.auth=auth(teacher);
        var data=resource.statistics(null);
        assertTrue(data.stream().anyMatch(s->s.id().equals(other.id.toString()) && s.schoolClass().equals("4OTHER")));
        var result=data.stream().filter(s->s.id().equals(student.id.toString())).findFirst().orElseThrow();
        assertEquals(1,result.plays().size());assertEquals(46,result.plays().getFirst().score());
        assertEquals(List.of(student.displayName),result.plays().getFirst().players());
        String json=mapper.valueToTree(result).toString();
        assertFalse(json.contains("Aussage"));assertFalse(json.contains("answerData"));
        resource.auth=auth(student);
        assertThrows(jakarta.ws.rs.ForbiddenException.class,()->resource.statistics(null));
    }

    private at.htlleonding.gamebasedlearning.auth.AuthProvider auth(AppUser user) {
        return new at.htlleonding.gamebasedlearning.auth.AuthProvider() {
            @Override public at.htlleonding.gamebasedlearning.auth.AuthenticatedUser currentUser(jakarta.ws.rs.core.HttpHeaders headers) {
                return new at.htlleonding.gamebasedlearning.auth.AuthenticatedUser(
                        user.externalSubject,user.username,user.displayName,user.schoolClass,user.role);
            }
        };
    }

    @Test void classAssignmentPersistsAndStatisticsIncludeStudentsWithoutGames() {
        AppUser teacher=user(UserRole.TEACHER),student=user(UserRole.STUDENT),noGames=user(UserRole.STUDENT),unassigned=user(UserRole.STUDENT);
        String className="5"+UUID.randomUUID().toString().substring(0,6).toUpperCase(Locale.ROOT);
        users.assignSchoolClass(student.id," "+className.toLowerCase(Locale.ROOT)+" ");
        users.assignSchoolClass(noGames.id,className);
        QuarkusTransaction.requiringNew().run(()->em.find(AppUser.class,unassigned.id).schoolClass=" ");
        Game game=game("Smart10",teacher);
        var round=solo.start(student.id,game.id);
        for(var card:round.questions().getFirst().cards())solo.answer(student.id,round.roundId(),card.id(),mapper.getNodeFactory().booleanNode(true));
        solo.finish(student.id,round.roundId());
        em.clear();
        assertEquals(className,users.getById(student.id).schoolClass);
        StatisticsResource resource=new StatisticsResource();resource.users=users;resource.em=em;resource.auth=auth(teacher);
        var data=resource.statistics(null);
        var members=data.stream().filter(s->s.schoolClass().equals(className)).toList();
        assertEquals(2,members.size());
        assertEquals(46,members.stream().flatMap(s->s.plays().stream()).mapToInt(StatisticsResource.Play::score).sum());
        assertTrue(members.stream().anyMatch(s->s.id().equals(noGames.id.toString()) && s.plays().isEmpty()));
        assertTrue(data.stream().anyMatch(s->s.id().equals(unassigned.id.toString()) && s.schoolClass().equals("Ohne Klasse")));
    }

    @Test void studentAdminsAppearInTheirClassWithScoresAndKeepAdminRights() {
        AppUser admin=user(UserRole.ADMIN), noGames=user(UserRole.ADMIN);
        AppUser student=user(UserRole.STUDENT), teacher=user(UserRole.TEACHER);
        String className="5"+UUID.randomUUID().toString().substring(0,6).toUpperCase(Locale.ROOT);
        users.assignSchoolClass(admin.id,className);
        users.assignSchoolClass(noGames.id,className);
        users.assignSchoolClass(student.id,className);
        QuarkusTransaction.requiringNew().run(()->{
            em.find(AppUser.class,admin.id).username="it220240";
            em.find(AppUser.class,noGames.id).username="it220269";
            em.find(AppUser.class,teacher.id).schoolClass=className;
        });
        Game game=game("Smart10",teacher);
        var round=solo.start(admin.id,game.id);
        for(var card:round.questions().getFirst().cards())solo.answer(admin.id,round.roundId(),card.id(),mapper.getNodeFactory().booleanNode(true));
        solo.finish(admin.id,round.roundId());
        em.clear();
        StatisticsResource resource=new StatisticsResource();resource.users=users;resource.em=em;
        resource.auth=auth(users.getById(admin.id));
        var data=resource.statistics(null);
        var members=data.stream().filter(s->s.schoolClass().equals(className)).toList();
        assertEquals(3,members.size());
        assertEquals(3,members.stream().map(StatisticsResource.Student::id).distinct().count());
        var adminResult=members.stream().filter(s->s.id().equals(admin.id.toString())).findFirst().orElseThrow();
        assertEquals(46,adminResult.plays().getFirst().score());
        assertEquals(46,members.stream().flatMap(s->s.plays().stream()).mapToInt(StatisticsResource.Play::score).sum());
        assertTrue(members.stream().anyMatch(s->s.id().equals(noGames.id.toString()) && s.plays().isEmpty()));
        assertFalse(data.stream().anyMatch(s->s.id().equals(teacher.id.toString())));
        assertEquals(UserRole.ADMIN,users.getById(admin.id).role);
    }

    @Test void storedCreateMineCopyAndStatusContractsUseOwnership() {
        AppUser owner=user(UserRole.TEACHER), stranger=user(UserRole.TEACHER);
        GameResource resource=new GameResource();resource.repository=games;resource.userService=users;
        resource.publicView=publicView;
        resource.authProvider=auth(owner);resource.mapper=mapper;
        Game input=game("Smart10",owner);
        Game created=(Game)resource.create(null, inputCopy(input)).getEntity();
        assertTrue(resource.mine(null).stream().anyMatch(g->g.id.equals(created.id)));
        QuarkusTransaction.requiringNew().run(()->resource.status(null,created.id,new GameResource.StatusRequest("PRIVATE")));
        em.clear(); // Direct resource calls share this test context; HTTP requests do not.
        assertFalse(resource.findAll().stream().anyMatch(g->g.path("id").asText().equals(created.id)));
        Game copied=resource.copy(null,created.id);
        assertNotEquals(created.id,copied.id);assertEquals("PRIVATE",copied.visibility);
        resource.authProvider=auth(stranger);
        assertThrows(jakarta.ws.rs.ForbiddenException.class,()->resource.update(null,created.id,created));
        assertThrows(jakarta.ws.rs.NotFoundException.class,()->resource.findById(null,created.id));
        assertFalse(resource.mine(null).stream().anyMatch(g->g.id.equals(created.id)));
    }
    private Game inputCopy(Game original) {
        Game input=new Game();input.id=UUID.randomUUID().toString();input.name="Erstellt";input.gameType="Smart10";
        Question q=new Question();q.text=original.questions.getFirst().text;q.type="TRUE_FALSE";
        q.smart10Cards=original.questions.getFirst().smart10Cards.stream().map(c->new Smart10Card(null,c.position(),c.text(),c.solution())).toList();
        input.questions.add(q);return input;
    }

    @Test void concurrentCreationReservesDifferentCodesAndUnboundConnectionsFail() throws Exception {
        MultiplayerGameService engine=engine();
        try {
            AppUser owner=user(UserRole.TEACHER);Game game=game("Smart10",owner);
            Client a=client(owner),b=client(owner);
            var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                var first=pool.submit(()->QuarkusTransaction.requiringNew().call(()->engine.createRoom(a.connection(),create(game,1,true)).get("code")));
                var second=pool.submit(()->QuarkusTransaction.requiringNew().call(()->engine.createRoom(b.connection(),create(game,1,true)).get("code")));
                assertNotEquals(first.get(),second.get());
            } finally {pool.shutdownNow();}
            tickets.unbind(a.connection().id());
            assertThrows(IllegalArgumentException.class,()->engine.createRoom(a.connection(),create(game,1,true)));
        } finally {engine.stopClock();}
    }
    @Test void transportAbortDoesNotCreateSessionAndTimerBoundsAreValidated() {
        MultiplayerGameService engine=engine();
        try {
            AppUser owner=user(UserRole.TEACHER);Client host=client(owner);Game game=game("Smart10",owner);
            assertThrows(IllegalArgumentException.class,()->engine.createRoom(host.connection(),create(game,1,true).put("turnSeconds",4)));
            assertThrows(IllegalArgumentException.class,()->engine.createRoom(host.connection(),create(game,1,true).put("turnSeconds",121)));
            String code=engine.createRoom(host.connection(),create(game,1,true)).get("code").toString();
            engine.startRoom(host.connection(),request(code));
            engine.leave(host.connection());
            assertEquals(0,QuarkusTransaction.requiringNew().call(()->((Number)em.createNativeQuery(
                    "select count(*) from game_sessions where game_id=?1").setParameter(1,game.id).getSingleResult()).intValue()));
        } finally {engine.stopClock();}
    }

    @Test void hostCanPlayAndTimeoutStoresZeroWithoutClientTrigger() throws Exception {
        MultiplayerGameService engine=engine();
        try {
            AppUser hostUser=user(UserRole.TEACHER);Client host=client(hostUser);
            Game game=game("Smart10",hostUser);
            String code=engine.createRoom(host.connection(),create(game,1,true)).get("code").toString();
            engine.startRoom(host.connection(),request(code));
            expire(engine,code,"turnDeadline");
            Method tick=MultiplayerGameService.class.getDeclaredMethod("tick");tick.setAccessible(true);tick.invoke(engine);
            JsonNode state=mapper.readTree(host.messages().getLast()).path("data");
            assertEquals("FINISHED",state.path("phase").asText());
            assertTrue(state.path("teams").get(0).path("eliminated").asBoolean());
            assertFalse(state.path("sessionId").isNull());
            assertEquals(0,store.totalPoints(hostUser.id));
            assertTrue(QuarkusTransaction.requiringNew().call(()->(Boolean)em.createNativeQuery(
                    "select host_participates from game_sessions where id=?1").setParameter(1,UUID.fromString(state.path("sessionId").asText())).getSingleResult()));
        } finally {engine.stopClock();}
    }

    @Test void randomTeamsBalancedLeadersBelongToTeamsAndEmptyTeamsRefused() {
        MultiplayerGameService engine=engine();
        try {
            AppUser owner=user(UserRole.TEACHER);Client host=client(owner);
            var input=create(game("Smart10",owner),3,false).put("assignmentMode","random");
            String code=engine.createRoom(host.connection(),input).get("code").toString();
            assertThrows(IllegalStateException.class,()->engine.startRoom(host.connection(),request(code)));
            for(int i=0;i<7;i++) engine.joinRoom(client(user(UserRole.STUDENT)).connection(),request(code));
            JsonNode state=mapper.valueToTree(engine.startRoom(host.connection(),request(code)));
            List<Integer> sizes=new ArrayList<>();
            state.path("teams").forEach(team->{sizes.add(team.path("members").size());
                assertTrue(team.path("members").toString().contains(team.path("leader").asText()));});
            assertEquals(1,Collections.max(sizes)-Collections.min(sizes));
            assertEquals(7,state.path("players").size());
        } finally {engine.stopClock();}
    }

    @Test void manualTeamChoiceRequiresExplicitUnevenApprovalAndExpiresAfter24Hours() throws Exception {
        MultiplayerGameService engine=engine();
        try {
            AppUser owner=user(UserRole.TEACHER);Client host=client(owner);
            String code=engine.createRoom(host.connection(),create(game("Smart10",owner),2,false)).get("code").toString();
            List<Client> players=new ArrayList<>();
            for(int i=0;i<4;i++){Client c=client(user(UserRole.STUDENT));players.add(c);engine.joinRoom(c.connection(),request(code));}
            for(int i=0;i<3;i++)engine.chooseTeam(host.connection(),request(code).put("playerId",players.get(i).connection().id()).put("team",0));
            engine.chooseTeam(host.connection(),request(code).put("playerId",players.get(3).connection().id()).put("team",1));
            assertThrows(IllegalArgumentException.class,()->engine.startRoom(host.connection(),request(code)));
            engine.startRoom(host.connection(),request(code).put("allowUneven",true));
            expire(engine,code,"createdAt");
            assertThrows(IllegalStateException.class,()->engine.pass(host.connection(),request(code)));
            assertTrue(host.messages().getLast().contains("room_closed"));
        } finally {engine.stopClock();}
    }

    @Test void justOneHidesWordsAndCorrectionsPersistExactlyOnce() throws Exception {
        MultiplayerGameService engine=engine();
        try {
            AppUser owner=user(UserRole.TEACHER);Client host=client(owner);
            String code=engine.createRoom(host.connection(),create(game("JustOne",owner),1,false)).get("code").toString();
            Client a=client(user(UserRole.STUDENT)),b=client(user(UserRole.STUDENT));
            engine.joinRoom(a.connection(),request(code));engine.joinRoom(b.connection(),request(code));
            JsonNode start=mapper.valueToTree(engine.startRoom(host.connection(),request(code)));
            String guesserId=start.path("justOne").path("guesser").asText();
            Client guesser=guesserId.equals(a.connection().id())?a:b;
            Client other=guesser==a?b:a;
            assertFalse(guesser.messages().getLast().contains("Geheim"));
            engine.justOne(guesser.connection(),request(code).put("action","number").put("number",2));
            assertFalse(guesser.messages().getLast().contains("Geheim"));
            assertTrue(other.messages().getLast().contains("Geheim2"));
            assertThrows(IllegalArgumentException.class,()->engine.justOne(guesser.connection(),request(code).put("action","clue").put("text","x")));
            engine.justOne(other.connection(),request(code).put("action","clue").put("text","Hinweis"));
            engine.justOne(guesser.connection(),request(code).put("action","guess").put("text","Geheim2"));
            JsonNode finished=mapper.valueToTree(engine.justOne(host.connection(),request(code).put("action","judge").put("correct",true)));
            assertEquals("FINISHED",finished.path("phase").asText());
            assertEquals(1,store.totalPoints(tickets.actor(a.connection().id()).userId()));
            String attempt=finished.path("justOne").path("attempts").get(0).path("id").asText();
            ObjectNode invalidate=request(code).put("action","invalidate").put("attemptId",attempt);
            engine.justOne(host.connection(),invalidate);engine.justOne(host.connection(),invalidate);
            assertEquals(0,store.totalPoints(tickets.actor(a.connection().id()).userId()));
            assertEquals(0,store.totalPoints(tickets.actor(b.connection().id()).userId()));
            engine.leave(b.connection());
            assertEquals(2,mapper.valueToTree(engine.retrySave(host.connection(),request(code))).path("players").size());
            assertThrows(jakarta.ws.rs.ForbiddenException.class,()->store.invalidateJustOne(UUID.fromString(finished.path("sessionId").asText()),UUID.fromString(attempt),tickets.actor(a.connection().id()).userId()));
        } finally {engine.stopClock();}
    }
    @Test void ticketReplayAndForeignPrivateGameAreRejected() {
        MultiplayerGameService engine=engine();
        try {
            AppUser owner=user(UserRole.TEACHER);Game game=game("Smart10",owner);
            QuarkusTransaction.requiringNew().run(()->em.find(Game.class,game.id).visibility="PRIVATE");
            Client stranger=client(user(UserRole.STUDENT));
            assertThrows(IllegalArgumentException.class,()->engine.createRoom(stranger.connection(),create(game,1,true)));
            String code=engine.createRoom(client(owner).connection(),create(game,1,true)).get("code").toString();
            assertThrows(IllegalArgumentException.class,()->engine.joinRoom(stranger.connection(),request(code)));
            String ticket=tickets.issue(owner);tickets.bind("once-"+UUID.randomUUID(),ticket);
            assertThrows(IllegalArgumentException.class,()->tickets.bind("again-"+UUID.randomUUID(),ticket));
        } finally {engine.stopClock();}
    }

    @Test void realWebSocketsBindBroadcastLobbyAndPersistFinishedGame() throws Exception {
        AppUser owner=user(UserRole.TEACHER), student=user(UserRole.STUDENT);
        Game game=game("Smart10",owner);
        var hostMessages=new java.util.concurrent.LinkedBlockingQueue<JsonNode>();
        var playerMessages=new java.util.concurrent.LinkedBlockingQueue<JsonNode>();
        java.net.http.WebSocket host=websocket(hostMessages),player=websocket(playerMessages);
        try {
            send(host,hostMessages,mapper.createObjectNode().put("type","bind_ticket").put("ticket",tickets.issue(owner)));
            JsonNode room=send(host,hostMessages,create(game,1,false).put("type","mp_create")).path("data");
            String code=room.path("code").asText();
            send(player,playerMessages,mapper.createObjectNode().put("type","bind_ticket").put("ticket",tickets.issue(student)));
            send(player,playerMessages,request(code).put("type","mp_join"));
            JsonNode joined=hostMessages.poll(5,java.util.concurrent.TimeUnit.SECONDS);
            assertNotNull(joined);assertEquals(1,joined.path("data").path("players").size());
            JsonNode started=send(host,hostMessages,request(code).put("type","mp_start")).path("data");
            String card=started.path("currentQuestion").path("cards").get(0).path("id").asText();
            send(player,playerMessages,request(code).put("type","mp_select_card").put("cardId",card));
            JsonNode ended=send(player,playerMessages,request(code).put("type","mp_answer").put("answer",false)).path("data");
            assertEquals("FINISHED",ended.path("phase").asText());
            assertTrue(ended.path("sessionId").isTextual());
            assertEquals(0,store.totalPoints(student.id));
        } finally {host.sendClose(1000,"done").join();player.sendClose(1000,"done").join();}
    }

    private java.net.http.WebSocket websocket(java.util.concurrent.BlockingQueue<JsonNode> messages) {
        return java.net.http.HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
                java.net.URI.create("ws://localhost:8081/user-socket"),new java.net.http.WebSocket.Listener(){
                    private final StringBuilder buffer=new StringBuilder();
                    @Override public void onOpen(java.net.http.WebSocket ws){ws.request(1);}
                    @Override public java.util.concurrent.CompletionStage<?> onText(java.net.http.WebSocket ws,CharSequence text,boolean last){
                        buffer.append(text);
                        if(last){try{messages.add(mapper.readTree(buffer.toString()));}catch(Exception e){throw new RuntimeException(e);}buffer.setLength(0);}
                        ws.request(1);return null;
                    }
                }).join();
    }
    private JsonNode send(java.net.http.WebSocket socket,java.util.concurrent.BlockingQueue<JsonNode> messages,ObjectNode request) throws Exception {
        String id=UUID.randomUUID().toString();request.put("requestId",id);
        socket.sendText(request.toString(),true).join();
        long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<until){
            JsonNode response=messages.poll(1,java.util.concurrent.TimeUnit.SECONDS);
            if(response!=null && id.equals(response.path("requestId").asText())){
                assertNotEquals("error",response.path("type").asText(),response.toString());return response;
            }
        }
        throw new AssertionError("WebSocket response timed out");
    }
}

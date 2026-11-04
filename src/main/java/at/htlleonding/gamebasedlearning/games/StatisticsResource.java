package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.auth.AuthProvider;
import at.htlleonding.gamebasedlearning.users.*;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;

@Path("/api/statistics")
@Produces(MediaType.APPLICATION_JSON)
public class StatisticsResource {
    @Inject AuthProvider auth;
    @Inject UserService users;
    @Inject EntityManager em;
    public record Play(String sessionId, String gameId, String game, String type, String mode,
                       String finishedAt, int score, List<String> players) { }
    public record Student(String id, String name, String schoolClass, List<Play> plays) { }
    public record AttemptView(String id, String team, boolean awarded, boolean invalidated) { }
    public record Hosted(String id, String title, String finishedAt, List<AttemptView> attempts) { }
    @GET
    @Path("/hosted")
    public List<Hosted> hosted(@Context HttpHeaders headers) {
        AppUser host = users.getOrCreateFromIdentity(auth.currentUser(headers));
        if (!host.active) throw new ForbiddenException();
        List<Object[]> sessions = em.createNativeQuery("select s.id,v.title,s.finished_at from game_sessions s "
                + "join game_versions v on v.id=s.game_version_id where s.host_user_id=?1 and v.game_type='JustOne' order by s.finished_at desc")
                .setParameter(1,host.id).getResultList();
        return sessions.stream().map(session -> {
            List<Object[]> attempts = em.createNativeQuery("select id,team_name,awarded,invalidated from just_one_attempts where session_id=?1 order by team_name,id")
                    .setParameter(1,session[0]).getResultList();
            return new Hosted(session[0].toString(),(String)session[1],session[2].toString(),attempts.stream()
                    .map(a->new AttemptView(a[0].toString(),(String)a[1],(Boolean)a[2],(Boolean)a[3])).toList());
        }).toList();
    }
    @GET
    public List<Student> statistics(@Context HttpHeaders headers) {
        AppUser viewer = users.getOrCreateFromIdentity(auth.currentUser(headers));
        if (!viewer.active || viewer.role == UserRole.STUDENT) throw new ForbiddenException();
        List<AppUser> students = em.createQuery("select u from AppUser u where u.role = :role order by u.schoolClass, u.displayName", AppUser.class)
                .setParameter("role", UserRole.STUDENT).getResultList();
        return students.stream().map(student -> {
            List<Object[]> rows = em.createNativeQuery("select s.id, s.game_id, v.title, v.game_type, s.mode, s.finished_at, p.score "
                    + "from session_participants p join game_sessions s on s.id=p.session_id "
                    + "join game_versions v on v.id=s.game_version_id where p.user_id=?1 order by s.finished_at desc")
                    .setParameter(1, student.id).getResultList();
            return new Student(student.id.toString(), student.displayName, student.schoolClass == null ? "Ohne Klasse" : student.schoolClass,
                    rows.stream().map(row -> {
                        List<String> players = em.createNativeQuery("select u.display_name from session_participants p "
                                + "join app_user u on u.id=p.user_id where p.session_id=?1 order by u.display_name", String.class)
                                .setParameter(1, row[0]).getResultList();
                        return new Play(row[0].toString(), (String)row[1], (String)row[2], (String)row[3],
                                (String)row[4], row[5].toString(), ((Number)row[6]).intValue(), players);
                    }).toList());
        }).toList();
    }
    @POST
    @Path("/sessions/{sessionId}/attempts/{attemptId}/invalidate")
    public void invalidate(@Context HttpHeaders headers, @PathParam("sessionId") UUID sessionId,
                           @PathParam("attemptId") UUID attemptId) {
        AppUser viewer = users.getOrCreateFromIdentity(auth.currentUser(headers));
        if (!viewer.active) throw new ForbiddenException();
        store.invalidateJustOne(sessionId, attemptId, viewer.id);
    }
    @Inject CompletedRoundStore store;
}

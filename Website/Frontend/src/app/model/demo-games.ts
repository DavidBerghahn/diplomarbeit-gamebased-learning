import { Game } from './game.model';

const gameBase = (id: string, name: string, description: string, question: Game['fragen'][number]): Game => ({
  id,
  spiel_typ: 'Smart10 / Quizbattle',
  lehrer: 'Demo Lehrer',
  name,
  beschreibung: description,
  fach: 'Programmieren',
  zweige: ['SEW', 'SSE'],
  erstellungsdatum: '2026-09-30',
  gespielte_runden: 0,
  fragen: [question],
});

const trueFalseQuestion: Game['fragen'][number] = {
  typ: 'SMART10',
  modus: 'TRUE_FALSE',
  frage: 'Dieser Begriff gehört zur objektorientierten Programmierung.',
  antwortmoeglichkeiten: [
    ['Klasse', true], ['Objekt', true], ['Vererbung', true], ['Polymorphie', true],
    ['Kapselung', true], ['Interface', true], ['Compiler', false], ['Variable', false],
    ['Schleife', false], ['Datenbank', false], ['Pixel', false], ['Router', false],
  ].map(([text, solution], index) => ({ id: index + 1, text: String(text), loesung: solution as boolean })),
};

const orderingQuestion: Game['fragen'][number] = {
  typ: 'SMART10',
  modus: 'ORDERING',
  frage: 'Ordne diese Programmiersprachen nach ihrem Entstehungsjahr (1 = älteste).',
  antwortmoeglichkeiten: [
    ['Fortran', 1], ['Lisp', 2], ['COBOL', 3], ['BASIC', 4],
    ['Pascal', 5], ['C', 6], ['SQL', 7], ['C++', 8],
    ['Python', 9], ['Java', 10], ['C#', 11], ['Kotlin', 12],
  ].map(([text, solution], index) => ({ id: index + 1, text: String(text), loesung: solution as number })),
};

const matchingQuestion: Game['fragen'][number] = {
  typ: 'SMART10',
  modus: 'FREE_TEXT',
  frage: 'Aus welchem Land stammt diese Programmiersprache?',
  antwortmoeglichkeiten: [
    ['Python', 'Niederlande'], ['Java', 'Kanada'], ['JavaScript', 'Vereinigte Staaten'],
    ['C', 'Vereinigte Staaten'], ['C++', 'Dänemark'], ['Ruby', 'Japan'],
    ['PHP', 'Dänemark'], ['Swift', 'Vereinigte Staaten'], ['Kotlin', 'Vereinigte Staaten'],
    ['Rust', 'Vereinigte Staaten'], ['Go', 'Vereinigte Staaten'], ['Pascal', 'Schweiz'],
  ].map(([text, solution], index) => ({ id: index + 1, text: String(text), loesung: String(solution) })),
};

export const DEMO_SMART10_GAMES: Game[] = [
  gameBase('demo-programming-true-false', 'Programmieren: Begriffe erkennen', 'Wahr oder falsch: Grundlagen der objektorientierten Programmierung.', trueFalseQuestion),
  gameBase('demo-programming-ordering', 'Programmieren: Zeitreise', 'Bringe bekannte Programmiersprachen in die richtige Reihenfolge.', orderingQuestion),
  gameBase('demo-programming-matching', 'Programmieren: Sprachzuordnung', 'Ordne Programmiersprachen ihrem Ursprungsland zu.', matchingQuestion),
];

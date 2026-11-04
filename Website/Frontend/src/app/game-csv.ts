import { Question, Smart10Mode } from './model/game.model';

export function parseGameCsv(content: string, justOne: boolean): Question[] {
  if (content.length > 2_000_000) throw new Error('Die CSV-Datei darf höchstens 2 MB groß sein.');
  const input = content.replace(/^\uFEFF/, '');
  const delimiter = input.split(/\r?\n/)[0].includes(';') ? ';' : ',';
  const rows: string[][] = [];
  let row: string[] = [],
    cell = '',
    quoted = false;
  for (let i = 0; i < input.length; i++) {
    const char = input[i];
    if (char === '"') {
      if (quoted && input[i + 1] === '"') {
        cell += '"';
        i++;
      } else quoted = !quoted;
    } else if (!quoted && (char === delimiter || char === '\n')) {
      row.push(cell.replace(/\r$/, '').trim());
      cell = '';
      if (char === '\n') {
        if (row.some(Boolean)) rows.push(row);
        row = [];
      }
    } else cell += char;
  }
  if (quoted) throw new Error('Ein Anführungszeichen in der CSV ist nicht geschlossen.');
  row.push(cell.replace(/\r$/, '').trim());
  if (row.some(Boolean)) rows.push(row);
  if (rows.length < 2 || rows.length > 5001) throw new Error('CSV benötigt 1–5000 Datenzeilen.');
  if (rows[0].join(';').toLowerCase() !== 'question;mode;position;text;solution')
    throw new Error(
      'Spalten müssen question;mode;position;text;solution heißen. Bitte Vorlage verwenden.',
    );
  const groups = new Map<
    string,
    {
      mode: string;
      entries: { position: number; text: string; solution: string | boolean | number }[];
    }
  >();
  for (const [index, fields] of rows.slice(1).entries()) {
    const [question, mode, position, text, solution] = fields;
    if (fields.length !== 5 || !question || !text)
      throw new Error('Unvollständige CSV-Zeile ' + (index + 2));
    if (!(justOne ? ['JUST_ONE'] : ['TRUE_FALSE', 'FREE_TEXT', 'ORDERING']).includes(mode))
      throw new Error('Ungültiger Modus in Zeile ' + (index + 2));
    const n = Number(position),
      count = justOne ? 5 : 12;
    if (!Number.isInteger(n) || n < 1 || n > count)
      throw new Error('Ungültige Position in Zeile ' + (index + 2));
    let value: string | boolean | number = solution;
    if (!justOne && mode === 'TRUE_FALSE') {
      if (!['true', 'false', 'wahr', 'falsch'].includes(solution.toLowerCase()))
        throw new Error('Wahr/Falsch-Lösung fehlt in Zeile ' + (index + 2));
      value = ['true', 'wahr'].includes(solution.toLowerCase());
    } else if (!justOne && mode === 'ORDERING') {
      value = Number(solution);
      if (!solution || !Number.isInteger(value) || value < 1 || value > 12)
        throw new Error('Rang muss 1–12 sein, Zeile ' + (index + 2));
    } else if (!justOne && !solution) throw new Error('Lösung fehlt in Zeile ' + (index + 2));
    const group = groups.get(question) ?? { mode, entries: [] };
    if (group.mode !== mode || group.entries.some((c) => c.position === n))
      throw new Error('Doppelte Position oder verschiedene Modi für ' + question);
    group.entries.push({ position: n, text, solution: value });
    groups.set(question, group);
  }
  return [...groups].map(([question, group]) => {
    if (group.entries.length !== (justOne ? 5 : 12))
      throw new Error('Falsche Kartenanzahl für ' + question);
    if (group.mode === 'ORDERING' && new Set(group.entries.map((c) => c.solution)).size !== 12)
      throw new Error('Reihenfolge muss jeden Rang von 1–12 genau einmal enthalten.');
    const cards = group.entries.sort((a, b) => a.position - b.position);
    return {
      typ: justOne ? 'JUST_ONE' : group.mode,
      modus: justOne ? undefined : (group.mode as Smart10Mode),
      frage: question,
      antwortmoeglichkeiten: justOne
        ? cards.map((c) => ({ text: c.text, ist_richtig: false }))
        : [],
      karten: justOne
        ? undefined
        : cards.map((c) => ({ position: c.position, text: c.text, loesung: c.solution })),
    };
  });
}

export function csvTemplate(mode: Smart10Mode | 'JUST_ONE'): string {
  const rows = ['question;mode;position;text;solution'];
  for (let i = 1; i <= (mode === 'JUST_ONE' ? 5 : 12); i++)
    rows.push(
      [
        'Programmieren',
        mode,
        String(i),
        'Begriff ' + i,
        mode === 'JUST_ONE'
          ? ''
          : mode === 'TRUE_FALSE'
            ? 'true'
            : mode === 'ORDERING'
              ? String(i)
              : 'Lösung ' + i,
      ].join(';'),
    );
  return '\uFEFF' + rows.join('\r\n');
}

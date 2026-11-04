import { csvTemplate, parseGameCsv } from './game-csv';

describe('CSV game import', () => {
  it.each(['TRUE_FALSE', 'FREE_TEXT', 'ORDERING', 'JUST_ONE'] as const)(
    'roundtrips the %s template',
    (mode) => {
      const result = parseGameCsv(csvTemplate(mode), mode === 'JUST_ONE');
      expect(result).toHaveLength(1);
      expect(mode === 'JUST_ONE' ? result[0].antwortmoeglichkeiten : result[0].karten).toHaveLength(
        mode === 'JUST_ONE' ? 5 : 12,
      );
    },
  );
  it('rejects duplicate ordering ranks and incomplete cards', () => {
    expect(() =>
      parseGameCsv(csvTemplate('ORDERING').replace('Begriff 12;12', 'Begriff 12;11'), false),
    ).toThrow(/Reihenfolge/);
    expect(() =>
      parseGameCsv(csvTemplate('TRUE_FALSE').split('\r\n').slice(0, -1).join('\r\n'), false),
    ).toThrow(/Kartenanzahl/);
  });
  it('supports quoted delimiters, escaped quotes, and multiline text', () => {
    const csv = csvTemplate('TRUE_FALSE').replace('Begriff 1;', '"Ein; ""Zitat""\nmehrzeilig";');
    expect(parseGameCsv(csv, false)[0].karten?.[0].text).toBe('Ein; "Zitat"\nmehrzeilig');
  });
});

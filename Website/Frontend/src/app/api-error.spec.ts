import { apiErrorMessage } from './api-error';

describe('API error messages', () => {
  it('shows the server message without the JSON wrapper', () => {
    expect(
      apiErrorMessage('{"message":"Dieses Spiel ist noch nicht spielbereit."}', 'Fehler'),
    ).toBe('Dieses Spiel ist noch nicht spielbereit.');
  });
  it('keeps plain text errors and uses a fallback for empty responses', () => {
    expect(apiErrorMessage('Nicht gefunden', 'Fehler')).toBe('Nicht gefunden');
    expect(apiErrorMessage('', 'Fehler')).toBe('Fehler');
  });
});

export function apiErrorMessage(body: string, fallback: string): string {
  try {
    const payload = JSON.parse(body);
    return typeof payload?.message === 'string' && payload.message.trim()
      ? payload.message
      : fallback;
  } catch {
    return body.trim() && !body.trim().startsWith('<') ? body : fallback;
  }
}

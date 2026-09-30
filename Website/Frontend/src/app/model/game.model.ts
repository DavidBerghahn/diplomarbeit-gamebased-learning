export interface AnswerOption {
  id?: number;
  text: string;
  ist_richtig?: boolean;
  loesung?: string | boolean | number;
}

export type GameStatus = 'DRAFT' | 'PUBLIC' | 'PRIVATE';
export type Smart10Mode = 'TRUE_FALSE' | 'FREE_TEXT' | 'ORDERING';

export interface Question {
  id?: number;
  typ: string;
  frage: string;
  antwortmoeglichkeiten: AnswerOption[];
  modus?: Smart10Mode;
  autor?: string;
  erstellungsdatum?: string;
  aenderungsdatum?: string;
  tags?: string[];
}

export interface Game {
  id: string;
  spiel_typ: string;
  lehrer: string;
  name: string;
  beschreibung?: string;
  fach?: string;
  zweige: string[];
  erstellungsdatum?: string;
  aenderungsdatum?: string;
  gespielte_runden: number;
  fragen: Question[];
  status?: GameStatus;
  besitzerId?: string;
  besitzer?: string;
  originalSpielId?: string;
}

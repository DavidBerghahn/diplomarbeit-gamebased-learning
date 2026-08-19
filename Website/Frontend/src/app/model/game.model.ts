export interface AnswerOption {
  id?: number;
  text: string;
  ist_richtig: boolean;
}

export interface Question {
  id?: number;
  typ: string;
  frage: string;
  antwortmoeglichkeiten: AnswerOption[];
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
  gespielte_runden: number;
  fragen: Question[];
}

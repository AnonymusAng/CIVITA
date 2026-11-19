/**
 * Agente de auditoría de CIVITA.
 * Motor experto determinista desarrollado para este proyecto: no llama modelos externos,
 * no transmite contenido y nunca afirma que un evento haya ocurrido realmente.
 */
const ENGINE_VERSION = "civita-audit-rules-1.0.0";
const CIVIC_TERMS = [
  "via", "cierre", "movilidad", "trafico", "transito", "congestion", "accidente",
  "semaforo", "senalizacion", "obra", "construccion", "hueco", "bache", "anden",
  "inundacion", "lluvia", "alcantarilla", "basura", "residuo", "arbol", "poda",
  "alumbrado", "luz", "ruido", "agua", "fuga", "parque", "espacio publico",
  "seguridad", "robo", "hurto", "amenaza", "incendio", "evento", "feria", "concierto",
  "transporte", "bus", "ruta", "peaton", "bicicleta", "contaminacion", "dano", "riesgo",
  "servicio", "barrio", "comunidad", "reporte", "emergencia", "animal", "cable", "poste"
];
const PERSONAL_DATA_PATTERNS = [
  /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/i,
  /(?:\+?57[\s.-]?)?3\d{2}[\s.-]?\d{3}[\s.-]?\d{4}\b/,
  /\b(?:cedula|c[eé]dula|documento|identificacion|identificaci[oó]n|c\.c\.)\s*(?:n(?:umero|úm(?:ero)?)?\.?\s*)?(?:de\s*)?\d{6,12}\b/i
];
const HARMFUL_PATTERNS = [
  /\b(?:voy a matar|vamos a matar|lo voy a asesinar|la voy a asesinar|amenaza de muerte|bomba|explosivo|suicid(?:arse|io))\b/,
  /\b(?:hijueputa|gonorrea|malparid[oa]|marica|estupid[oa]|imb[eé]cil|idiota)\b/
];
const URL_PATTERN = /https?:\/\//gi;
const REPEATED_CHAR_PATTERN = /(.)\1{7,}/;
const MIN_AUTO_PUBLISH_SCORE = 65;

function normalizeText(value) {
  return String(value || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
}

function result(decision, reasonCode, summary, score, signals = []) {
  return { decision, reasonCode, summary: String(summary).replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim().slice(0, 180), score, signals, engineVersion: ENGINE_VERSION };
}

function auditReport(report = {}) {
  const title = typeof report.title === "string" ? report.title.trim().slice(0, 120) : "";
  const description = typeof report.description === "string" ? report.description.trim().slice(0, 2000) : "";
  const category = typeof report.categoryName === "string" ? report.categoryName.trim().slice(0, 80) : "";
  const locationReference = typeof report.locationReference === "string" ? report.locationReference.trim().slice(0, 200) : "";
  const content = normalizeText(`${title} ${description} ${locationReference}`);

  if (PERSONAL_DATA_PATTERNS.some(pattern => pattern.test(`${title} ${description}`))) {
    return result("manual_review", "possible_personal_data", "El texto podría exponer datos personales y requiere revisión humana.", 0, ["personal_data"]);
  }
  if (HARMFUL_PATTERNS.some(pattern => pattern.test(content))) {
    return result("manual_review", "potentially_harmful_content", "El contenido requiere una revisión humana por seguridad y convivencia.", 0, ["safety_review"]);
  }
  if (title.length < 5 || description.length < 10 || !category || locationReference.length < 3) {
    return result("manual_review", "insufficient_information", "Faltan datos básicos para evaluar el reporte; requiere revisión humana.", 0, ["incomplete"]);
  }
  if (REPEATED_CHAR_PATTERN.test(content) || (content.match(URL_PATTERN) || []).length > 1) {
    return result("manual_review", "possible_spam", "Se detectó una señal de formato repetitivo o enlaces y el reporte requiere revisión humana.", 0, ["spam_signal"]);
  }

  const words = content.match(/[a-z0-9]{3,}/g) || [];
  const uniqueRatio = words.length ? new Set(words).size / words.length : 0;
  const civicTerm = CIVIC_TERMS.find(term => content.includes(normalizeText(term)));
  let score = 0;
  const signals = [];
  if (title.length >= 12) { score += 15; signals.push("title_descriptive"); }
  else { score += 7; signals.push("title_short"); }
  if (description.length >= 40) { score += 25; signals.push("description_detailed"); }
  else { score += 10; signals.push("description_short"); }
  if (locationReference.length >= 6) { score += 15; signals.push("location_reference"); }
  else { score += 8; signals.push("location_brief"); }
  if (uniqueRatio >= 0.50) { score += 10; signals.push("text_varied"); }
  else { signals.push("text_repetitive"); }
  if (civicTerm) { score += 25; signals.push("civic_topic"); }
  else { signals.push("civic_topic_unclear"); }

  if (!civicTerm || score < MIN_AUTO_PUBLISH_SCORE) {
    return result("manual_review", civicTerm ? "insufficient_information" : "unclear_civic_relevance",
      civicTerm ? "La información es breve o poco específica; requiere revisión humana." : "No se identificó con claridad un asunto ciudadano; requiere revisión humana.", score, signals);
  }
  return result("visible", "content_eligible", "El reporte cumple señales básicas de claridad y pertinencia. Esta revisión no confirma los hechos.", score, signals);
}

function fallbackAssessment() {
  return result("manual_review", "audit_engine_error", "La auditoría automática no pudo completarse. El reporte requiere revisión humana.", 0, ["engine_error"]);
}

module.exports = { ENGINE_VERSION, MIN_AUTO_PUBLISH_SCORE, auditReport, fallbackAssessment, normalizeText };

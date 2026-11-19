const test = require("node:test");
const assert = require("node:assert/strict");
const { auditReport, fallbackAssessment, ENGINE_VERSION } = require("../auditPolicy");

const clearReport = () => ({
  title: "Hueco profundo en la vía del barrio",
  description: "Hay un hueco grande junto al parque y dificulta el paso de peatones y bicicletas desde esta mañana.",
  categoryName: "Movilidad",
  locationReference: "Barrio La Esmeralda, cerca del parque",
});

test("project-owned rule engine approves a clear civic report without external AI", () => {
  const result = auditReport(clearReport());
  assert.equal(result.decision, "visible");
  assert.equal(result.reasonCode, "content_eligible");
  assert.equal(result.engineVersion, ENGINE_VERSION);
  assert.ok(result.score >= 65);
  assert.match(result.summary, /no confirma los hechos/);
});

test("incomplete or unrelated report is routed to human review", () => {
  assert.equal(auditReport({ title: "Cierre", description: "No sé", categoryName: "Movilidad", locationReference: "Centro" }).decision, "manual_review");
  assert.equal(auditReport({ title: "Venta de zapatos nuevos", description: "Compre hoy zapatos baratos con descuentos y envíos a toda Colombia", categoryName: "Eventos", locationReference: "Popayán" }).reasonCode, "unclear_civic_relevance");
});

test("possible personal data and harmful text are never auto-published", () => {
  assert.equal(auditReport({ ...clearReport(), description: "Llamen al 3001234567 porque hay un hueco en la vía junto al parque." }).reasonCode, "possible_personal_data");
  assert.equal(auditReport({ ...clearReport(), description: "Voy a matar a quien reporte este hueco, esto pasa en el parque." }).reasonCode, "potentially_harmful_content");
});

test("spam indicators are queued instead of rejected", () => {
  assert.equal(auditReport({ ...clearReport(), description: "Hueco hueco hueco hueco hueco hueco https://a.test https://b.test" }).decision, "manual_review");
});

test("engine failure defaults to human review", () => {
  assert.equal(fallbackAssessment().decision, "manual_review");
});

const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const { ENGINE_VERSION, auditReport, fallbackAssessment } = require("./auditPolicy");

initializeApp();
const db = getFirestore();

async function claimAudit(ref) {
  return db.runTransaction(async (tx) => {
    const snapshot = await tx.get(ref);
    if (!snapshot.exists) return false;
    const data = snapshot.data();
    const staleProcessing = data.auditState === "processing" &&
      Date.now() - Number(data.auditStartedAt || 0) > 90000;
    if (data.auditState !== "pending" && !staleProcessing) return false;
    tx.update(ref, {
      auditState: "processing",
      auditStartedAt: Date.now(),
      auditUpdatedAt: FieldValue.serverTimestamp(),
    });
    return true;
  });
}

async function runAudit(ref, report) {
  if (!(await claimAudit(ref))) return null;
  try {
    const assessment = auditReport(report);
    await ref.update({
      auditState: assessment.decision,
      auditSummary: assessment.summary,
      auditReasonCode: assessment.reasonCode,
      auditScore: assessment.score,
      auditSignals: assessment.signals,
      auditEngineVersion: assessment.engineVersion,
      auditUpdatedAt: FieldValue.serverTimestamp(),
      auditStartedAt: FieldValue.delete(),
    });
    return assessment;
  } catch (error) {
    // No se registran título, descripción, UID, correo ni ubicación en logs.
    logger.error("CIVITA audit engine failed", { message: error.message });
    const fallback = fallbackAssessment();
    await ref.update({
      auditState: fallback.decision,
      auditSummary: fallback.summary,
      auditReasonCode: fallback.reasonCode,
      auditScore: fallback.score,
      auditSignals: fallback.signals,
      auditEngineVersion: ENGINE_VERSION,
      auditUpdatedAt: FieldValue.serverTimestamp(),
      auditStartedAt: FieldValue.delete(),
    });
    return fallback;
  }
}

exports.auditNewReport = onDocumentCreated({
  document: "reports/{reportId}",
  region: "us-central1",
  timeoutSeconds: 60,
  memory: "256MiB",
  maxInstances: 3,
  retry: true,
}, async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;
  await runAudit(snapshot.ref, snapshot.data());
});

module.exports.runAudit = runAudit;

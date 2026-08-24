import "server-only";
import { Document, Page, View, Text, Image, StyleSheet, renderToBuffer } from "@react-pdf/renderer";
import { prisma } from "@/lib/db";
import { readUploadedFile } from "@/lib/uploads";

const TYPE_LABELS: Record<string, string> = {
  MONTAZA: "Montaža",
  DEMONTAZA: "Demontaža",
  INTERVENCIJA: "Intervencija",
  PREMONTAZA: "Premontaža",
  OSTALO: "Ostalo",
};
const DIFFICULTY_LABELS: Record<string, string> = { OSNOVNA: "Osnovna", ZAHTEVNA: "Zahtevna" };
const CULPRIT_LABELS: Record<string, string> = { SLEDENJE: "Sledenje", STRANKA: "Stranka" };
const OPTION_LABELS: Record<string, string> = {
  DIN1: "DIN1 (IGN)",
  ALL_CAN: "ALL CAN",
  WIRE_TEMP1: "1 Wire Temp (1)",
  WIRE_TEMP2: "1 Wire Temp (2)",
  WIRE_TEMP3: "1 Wire Temp (3)",
  ID_KEY: "ID",
  RFID_125: "RFID 125 kHz",
  RFID_1356: "RFID 13,56 MHz",
  BUZZER: "Brenčač",
};
// Enako kot v nalogi/[id]/page.tsx in nalogi/page.tsx -- ostali modeli (FMC130 ipd.) se
// prikažejo kot so, samo OSTALO ima svojo oznako.
const DEVICE_MODEL_LABELS: Record<string, string> = { OSTALO: "Drugo" };

const styles = StyleSheet.create({
  page: { padding: 36, paddingBottom: 46, fontSize: 10.5, fontFamily: "Helvetica", color: "#1a1a1a" },
  headerRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 4 },
  title: { fontSize: 20, fontFamily: "Helvetica-Bold" },
  subtitle: { fontSize: 10, marginTop: 2, color: "#555" },
  cancelledBadge: {
    fontSize: 11,
    fontFamily: "Helvetica-Bold",
    color: "#b91c1c",
    borderWidth: 1,
    borderColor: "#b91c1c",
    borderRadius: 3,
    paddingVertical: 3,
    paddingHorizontal: 8,
  },
  headerRule: { borderBottomWidth: 2, borderBottomColor: "#111", marginBottom: 14 },
  section: {
    marginBottom: 10,
    borderWidth: 1,
    borderColor: "#ddd",
    borderRadius: 4,
    padding: 10,
  },
  sectionTitle: {
    fontSize: 12,
    marginBottom: 7,
    fontFamily: "Helvetica-Bold",
    textTransform: "uppercase",
    letterSpacing: 0.5,
    color: "#333",
  },
  row: { flexDirection: "row", marginBottom: 5 },
  label: { width: 130, color: "#666" },
  value: { flex: 1, fontFamily: "Helvetica-Bold" },
  twoCol: { flexDirection: "row", gap: 24 },
  col: { flex: 1 },
  optionsGrid: { flexDirection: "row", flexWrap: "wrap" },
  optionItem: { width: "33%", marginBottom: 5 },
  listItem: { marginBottom: 4 },
  photosGrid: { flexDirection: "row", flexWrap: "wrap", gap: 10, marginTop: 4 },
  photoItem: { width: 130 },
  photo: { width: 130, height: 95, objectFit: "cover", borderWidth: 1, borderColor: "#ccc" },
  photoCaption: { fontSize: 8, color: "#777", marginTop: 3, textAlign: "center" },
  signature: { width: 220, height: 80, marginTop: 4, borderWidth: 1, borderColor: "#ccc" },
  signatureMeta: { fontSize: 8.5, color: "#777", marginTop: 4 },
  footer: {
    position: "absolute",
    bottom: 24,
    left: 36,
    right: 36,
    flexDirection: "row",
    justifyContent: "space-between",
    fontSize: 8,
    color: "#999",
    borderTopWidth: 1,
    borderTopColor: "#ddd",
    paddingTop: 6,
  },
});

function Field({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.row}>
      <Text style={styles.label}>{label}</Text>
      <Text style={styles.value}>{value}</Text>
    </View>
  );
}

export async function generateWorkOrderPdf(workOrderId: string): Promise<Buffer> {
  const order = await prisma.workOrder.findUniqueOrThrow({
    where: { id: workOrderId },
    include: { client: true, installers: true, options: true, deviceModels: true, photos: true, createdBy: true },
  });

  const photoBuffers = await Promise.all(
    order.photos.map(async (p) => ({
      id: p.id,
      data: await readUploadedFile(p.filePath),
      takenAt: p.takenAt ?? p.uploadedAt,
    }))
  );
  const signatureBuffer = order.signatureUrl ? await readUploadedFile(order.signatureUrl) : null;

  const installerText = order.installers
    .map((i) => (i.name === "OSTALO" ? i.otherText ?? "Ostalo" : i.name.charAt(0) + i.name.slice(1).toLowerCase()))
    .join(", ");

  const doc = (
    <Document>
      <Page size="A4" style={styles.page}>
        <View style={styles.headerRow}>
          <View>
            <Text style={styles.title}>Delovni nalog {order.ident}</Text>
            <Text style={styles.subtitle}>{order.orderDate.toLocaleString("sl-SI")}</Text>
          </View>
          {order.status === "CANCELLED" && <Text style={styles.cancelledBadge}>PREKLICANO</Text>}
        </View>
        <View style={styles.headerRule} />

        <View style={styles.section} wrap={false}>
          <Text style={styles.sectionTitle}>Osnovni podatki</Text>
          <View style={styles.twoCol}>
            <View style={styles.col}>
              <Field label="Tip" value={TYPE_LABELS[order.type] ?? order.type} />
              <Field label="Zahtevnost" value={DIFFICULTY_LABELS[order.difficulty] ?? order.difficulty} />
              <Field label="Monterji" value={installerText || "—"} />
            </View>
            <View style={styles.col}>
              <Field label="Stranka" value={order.client.name} />
              {order.client.address && <Field label="Naslov" value={order.client.address} />}
              {order.client.contactInfo && <Field label="Kontakt" value={order.client.contactInfo} />}
            </View>
          </View>
        </View>

        <View style={styles.section} wrap={false}>
          <Text style={styles.sectionTitle}>Vozilo</Text>
          <View style={styles.twoCol}>
            <View style={styles.col}>
              <Field label="Registrska št." value={order.vehiclePlate} />
              <Field label="Vozilo" value={`${order.vehicleBrand} ${order.vehicleModel} (${order.vehicleYear})`} />
            </View>
            <View style={styles.col}>
              <Field label="IMEI" value={order.imei} />
              {order.imeiPrev && <Field label="IMEI prej" value={order.imeiPrev} />}
              {order.culprit && <Field label="Krivec" value={CULPRIT_LABELS[order.culprit] ?? order.culprit} />}
            </View>
          </View>
        </View>

        {(order.options.length > 0 || order.deviceModels.length > 0) && (
          <View style={styles.section} wrap={false}>
            <Text style={styles.sectionTitle}>Naprava in oprema</Text>
            {order.deviceModels.length > 0 && (
              <View style={{ marginBottom: order.options.length > 0 ? 8 : 0 }}>
                {order.deviceModels.map((dm) => (
                  <Text key={dm.id} style={styles.listItem}>
                    {DEVICE_MODEL_LABELS[dm.deviceModel] ?? dm.deviceModel}
                    {dm.comment ? `: ${dm.comment}` : ""}
                  </Text>
                ))}
              </View>
            )}
            {order.options.length > 0 && (
              <View style={styles.optionsGrid}>
                {order.options.map((o) => (
                  <Text key={o.id} style={styles.optionItem}>
                    {OPTION_LABELS[o.optionType] ?? o.optionType}
                    {o.comment ? `: ${o.comment}` : ""}
                  </Text>
                ))}
              </View>
            )}
          </View>
        )}

        {order.comment && (
          <View style={styles.section} wrap={false}>
            <Text style={styles.sectionTitle}>Komentar</Text>
            <Text>{order.comment}</Text>
          </View>
        )}

        {photoBuffers.length > 0 && (
          <View style={styles.section} wrap={false}>
            <Text style={styles.sectionTitle}>Slike ({photoBuffers.length})</Text>
            <View style={styles.photosGrid}>
              {photoBuffers.map((p) => (
                <View key={p.id} style={styles.photoItem}>
                  <Image style={styles.photo} src={{ data: p.data, format: "jpg" }} />
                  <Text style={styles.photoCaption}>{p.takenAt.toLocaleString("sl-SI")}</Text>
                </View>
              ))}
            </View>
          </View>
        )}

        {signatureBuffer && (
          <View style={styles.section} wrap={false}>
            <Text style={styles.sectionTitle}>Podpis stranke</Text>
            <Image style={styles.signature} src={{ data: signatureBuffer, format: "png" }} />
            {order.signedAt && (
              <Text style={styles.signatureMeta}>Podpisano: {order.signedAt.toLocaleString("sl-SI")}</Text>
            )}
          </View>
        )}

        <View style={styles.footer} fixed>
          <Text>Izdelal: {order.createdBy.fullName}</Text>
          <Text render={({ pageNumber, totalPages }) => `Stran ${pageNumber} / ${totalPages}`} />
        </View>
      </Page>
    </Document>
  );

  return renderToBuffer(doc);
}

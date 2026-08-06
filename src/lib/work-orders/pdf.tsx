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
const OPTION_LABELS: Record<string, string> = {
  ALL_CAN: "ALL CAN",
  WIRE_TEMP1: "1 Wire Temp (1)",
  WIRE_TEMP2: "1 Wire Temp (2)",
  WIRE_TEMP3: "1 Wire Temp (3)",
  ID_KEY: "ID",
  RFID_125: "RFID 125 kHz",
  RFID_1356: "RFID 13,56 MHz",
  BUZZER: "Brenčač",
};

const styles = StyleSheet.create({
  page: { padding: 32, fontSize: 10, fontFamily: "Helvetica" },
  title: { fontSize: 16, marginBottom: 4, fontFamily: "Helvetica-Bold" },
  subtitle: { fontSize: 10, marginBottom: 16, color: "#555" },
  section: { marginBottom: 12 },
  sectionTitle: { fontSize: 11, marginBottom: 4, fontFamily: "Helvetica-Bold" },
  row: { flexDirection: "row", marginBottom: 2 },
  label: { width: 110, color: "#555" },
  value: { flex: 1 },
  optionsGrid: { flexDirection: "row", flexWrap: "wrap" },
  optionItem: { width: "33%", marginBottom: 3 },
  photosGrid: { flexDirection: "row", flexWrap: "wrap", gap: 6, marginTop: 4 },
  photo: { width: 140, height: 100, objectFit: "cover" },
  signature: { width: 220, height: 90, marginTop: 4, borderWidth: 1, borderColor: "#ccc" },
});

export async function generateWorkOrderPdf(workOrderId: string): Promise<Buffer> {
  const order = await prisma.workOrder.findUniqueOrThrow({
    where: { id: workOrderId },
    include: { client: true, installers: true, options: true, photos: true, createdBy: true },
  });

  const photoBuffers = await Promise.all(
    order.photos.map(async (p) => ({ id: p.id, data: await readUploadedFile(p.filePath) }))
  );
  const signatureBuffer = order.signatureUrl ? await readUploadedFile(order.signatureUrl) : null;

  const installerText = order.installers
    .map((i) => (i.name === "OSTALO" ? i.otherText ?? "Ostalo" : i.name.charAt(0) + i.name.slice(1).toLowerCase()))
    .join(", ");

  const doc = (
    <Document>
      <Page size="A4" style={styles.page}>
        <Text style={styles.title}>Delovni nalog {order.ident}</Text>
        <Text style={styles.subtitle}>{order.orderDate.toLocaleDateString("sl-SI")}</Text>

        <View style={styles.section}>
          <View style={styles.row}>
            <Text style={styles.label}>Tip</Text>
            <Text style={styles.value}>{TYPE_LABELS[order.type] ?? order.type}</Text>
          </View>
          <View style={styles.row}>
            <Text style={styles.label}>Zahtevnost</Text>
            <Text style={styles.value}>{DIFFICULTY_LABELS[order.difficulty] ?? order.difficulty}</Text>
          </View>
          <View style={styles.row}>
            <Text style={styles.label}>Stranka</Text>
            <Text style={styles.value}>{order.client.name}</Text>
          </View>
          <View style={styles.row}>
            <Text style={styles.label}>Monterji</Text>
            <Text style={styles.value}>{installerText}</Text>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Vozilo</Text>
          <View style={styles.row}>
            <Text style={styles.label}>Registrska št.</Text>
            <Text style={styles.value}>{order.vehiclePlate}</Text>
          </View>
          <View style={styles.row}>
            <Text style={styles.label}>Vozilo</Text>
            <Text style={styles.value}>
              {order.vehicleBrand} {order.vehicleModel} ({order.vehicleYear})
            </Text>
          </View>
          <View style={styles.row}>
            <Text style={styles.label}>IMEI</Text>
            <Text style={styles.value}>{order.imei}</Text>
          </View>
          {order.imeiPrev && (
            <View style={styles.row}>
              <Text style={styles.label}>IMEI prej</Text>
              <Text style={styles.value}>{order.imeiPrev}</Text>
            </View>
          )}
        </View>

        {order.options.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>DIN / ANI / CAN</Text>
            <View style={styles.optionsGrid}>
              {order.options.map((o) => (
                <Text key={o.id} style={styles.optionItem}>
                  {OPTION_LABELS[o.optionType] ?? o.optionType}
                  {o.comment ? `: ${o.comment}` : ""}
                </Text>
              ))}
            </View>
          </View>
        )}

        {order.comment && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Komentar</Text>
            <Text>{order.comment}</Text>
          </View>
        )}

        {photoBuffers.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Slike</Text>
            <View style={styles.photosGrid}>
              {photoBuffers.map((p) => (
                <Image key={p.id} style={styles.photo} src={{ data: p.data, format: "jpg" }} />
              ))}
            </View>
          </View>
        )}

        {signatureBuffer && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Podpis stranke</Text>
            <Image style={styles.signature} src={{ data: signatureBuffer, format: "png" }} />
          </View>
        )}

        <Text style={{ marginTop: 16, color: "#999", fontSize: 8 }}>Izdelal: {order.createdBy.fullName}</Text>
      </Page>
    </Document>
  );

  return renderToBuffer(doc);
}

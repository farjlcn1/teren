import { Document, Page, Text, View, Image, StyleSheet } from "@react-pdf/renderer";
import type { Client, WorkOrder, WorkOrderInstaller, WorkOrderOption, WorkOrderPhoto } from "@prisma/client";
import {
  WORK_ORDER_TYPE_LABELS,
  DIFFICULTY_LABELS,
  INSTALLER_NAME_LABELS,
  OPTION_TYPE_LABELS,
} from "./constants";

export type FullWorkOrder = WorkOrder & {
  client: Client;
  installers: WorkOrderInstaller[];
  options: WorkOrderOption[];
  photos: WorkOrderPhoto[];
};

const styles = StyleSheet.create({
  page: { padding: 32, fontSize: 10, fontFamily: "Helvetica" },
  title: { fontSize: 16, fontWeight: 700, marginBottom: 4 },
  subtitle: { fontSize: 10, color: "#555", marginBottom: 16 },
  row: { flexDirection: "row", marginBottom: 4 },
  label: { width: 130, color: "#555" },
  value: { flex: 1 },
  sectionTitle: { fontSize: 12, fontWeight: 700, marginTop: 16, marginBottom: 6 },
  optionRow: { flexDirection: "row", marginBottom: 2 },
  photoGrid: { flexDirection: "row", flexWrap: "wrap", gap: 8, marginTop: 6 },
  photo: { width: 150, height: 110, objectFit: "cover" },
  signature: { width: 220, height: 90, marginTop: 6, border: "1px solid #ddd" },
});

type Props = {
  order: FullWorkOrder;
  photoBuffers: { buffer: Buffer; format: "jpg" | "png" }[];
  signatureBuffer: Buffer | null;
};

export function WorkOrderPdfDocument({ order, photoBuffers, signatureBuffer }: Props) {
  return (
    <Document>
      <Page size="A4" style={styles.page}>
        <Text style={styles.title}>Delovni nalog {order.ident}</Text>
        <Text style={styles.subtitle}>{order.orderDate.toLocaleDateString("sl-SI")}</Text>

        <View style={styles.row}>
          <Text style={styles.label}>Stranka</Text>
          <Text style={styles.value}>{order.client.name}</Text>
        </View>
        <View style={styles.row}>
          <Text style={styles.label}>Tip naloga</Text>
          <Text style={styles.value}>{WORK_ORDER_TYPE_LABELS[order.type]}</Text>
        </View>
        <View style={styles.row}>
          <Text style={styles.label}>Zahtevnost</Text>
          <Text style={styles.value}>{DIFFICULTY_LABELS[order.difficulty]}</Text>
        </View>
        <View style={styles.row}>
          <Text style={styles.label}>Monterji</Text>
          <Text style={styles.value}>
            {order.installers
              .map((i) => (i.name === "OSTALO" ? i.otherText : INSTALLER_NAME_LABELS[i.name]))
              .join(", ")}
          </Text>
        </View>
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

        {order.options.length > 0 && (
          <View>
            <Text style={styles.sectionTitle}>Izbire</Text>
            {order.options.map((o) => (
              <View key={o.id} style={styles.optionRow}>
                <Text style={{ width: 90, fontWeight: 700 }}>{OPTION_TYPE_LABELS[o.optionType]}</Text>
                <Text>{o.comment || ""}</Text>
              </View>
            ))}
          </View>
        )}

        {order.comment && (
          <View>
            <Text style={styles.sectionTitle}>Komentar</Text>
            <Text>{order.comment}</Text>
          </View>
        )}

        {photoBuffers.length > 0 && (
          <View>
            <Text style={styles.sectionTitle}>Slike</Text>
            <View style={styles.photoGrid}>
              {photoBuffers.map((p, i) => (
                <Image key={i} style={styles.photo} src={{ data: p.buffer, format: p.format }} />
              ))}
            </View>
          </View>
        )}

        {signatureBuffer && (
          <View>
            <Text style={styles.sectionTitle}>Podpis stranke</Text>
            <Image style={styles.signature} src={{ data: signatureBuffer, format: "png" }} />
          </View>
        )}
      </Page>
    </Document>
  );
}

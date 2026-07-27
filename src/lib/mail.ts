import "server-only";
import nodemailer from "nodemailer";
import { generateWorkOrderPdf } from "@/lib/work-orders/pdf";
import { prisma } from "@/lib/db";

function getTransport() {
  const { SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD } = process.env;
  if (!SMTP_HOST || !SMTP_USER || !SMTP_PASSWORD) {
    throw new Error("SMTP ni nastavljen (SMTP_HOST/SMTP_USER/SMTP_PASSWORD v .env).");
  }
  return nodemailer.createTransport({
    host: SMTP_HOST,
    port: Number(SMTP_PORT || 587),
    secure: Number(SMTP_PORT || 587) === 465,
    auth: { user: SMTP_USER, pass: SMTP_PASSWORD },
  });
}

export async function sendWorkOrderEmail(workOrderId: string, toEmail: string) {
  const order = await prisma.workOrder.findUniqueOrThrow({
    where: { id: workOrderId },
    include: { client: true },
  });

  const pdfBuffer = await generateWorkOrderPdf(workOrderId);
  const transport = getTransport();

  await transport.sendMail({
    from: process.env.SMTP_FROM || process.env.SMTP_USER,
    to: toEmail,
    subject: `Delovni nalog ${order.ident} — ${order.client.name}`,
    text: `V prilogi je delovni nalog ${order.ident} za stranko ${order.client.name}.`,
    attachments: [{ filename: `nalog-${order.ident}.pdf`, content: pdfBuffer }],
  });

  await prisma.workOrder.update({ where: { id: workOrderId }, data: { sentAt: new Date() } });
}

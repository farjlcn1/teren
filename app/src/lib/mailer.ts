import nodemailer from "nodemailer";

let transporter: nodemailer.Transporter | null = null;

function getTransporter() {
  if (transporter) return transporter;

  transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port: Number(process.env.SMTP_PORT || 587),
    secure: Number(process.env.SMTP_PORT) === 465,
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASSWORD,
    },
  });

  return transporter;
}

export async function sendWorkOrderEmail(to: string, ident: string, pdfBuffer: Buffer) {
  const transport = getTransporter();

  await transport.sendMail({
    from: process.env.SMTP_FROM,
    to,
    subject: `Delovni nalog ${ident}`,
    text: `V prilogi je delovni nalog št. ${ident}.`,
    attachments: [{ filename: `${ident}.pdf`, content: pdfBuffer }],
  });
}

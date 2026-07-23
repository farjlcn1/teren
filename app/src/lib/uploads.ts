import { mkdir, writeFile, readFile } from "fs/promises";
import path from "path";
import sharp from "sharp";
import type { WorkOrderPhoto } from "@prisma/client";

const UPLOAD_DIR = process.env.UPLOAD_DIR || path.join(process.cwd(), "uploads");

export function uploadDirFor(workOrderId: string) {
  return path.join(UPLOAD_DIR, workOrderId);
}

export function publicUrlFor(workOrderId: string, fileName: string) {
  return `/api/files/${workOrderId}/${fileName}`;
}

export async function saveCompressedPhoto(workOrderId: string, index: number, file: File): Promise<string> {
  const dir = uploadDirFor(workOrderId);
  await mkdir(dir, { recursive: true });

  const buffer = Buffer.from(await file.arrayBuffer());
  const fileName = `foto-${index + 1}.jpg`;
  const filePath = path.join(dir, fileName);

  await sharp(buffer)
    .rotate()
    .resize({ width: 1600, withoutEnlargement: true })
    .jpeg({ quality: 80 })
    .toFile(filePath);

  return publicUrlFor(workOrderId, fileName);
}

export async function saveSignature(workOrderId: string, file: File): Promise<string> {
  const dir = uploadDirFor(workOrderId);
  await mkdir(dir, { recursive: true });

  const buffer = Buffer.from(await file.arrayBuffer());
  const fileName = "podpis.png";
  const filePath = path.join(dir, fileName);

  await writeFile(filePath, buffer);

  return publicUrlFor(workOrderId, fileName);
}

export async function readPhotoBuffers(
  workOrderId: string,
  photos: WorkOrderPhoto[]
): Promise<{ buffer: Buffer; format: "jpg" | "png" }[]> {
  const dir = uploadDirFor(workOrderId);
  const buffers: { buffer: Buffer; format: "jpg" | "png" }[] = [];

  for (const photo of photos) {
    const fileName = photo.fileUrl.split("/").pop() ?? "";
    try {
      const buffer = await readFile(path.join(dir, fileName));
      buffers.push({ buffer, format: fileName.endsWith(".png") ? "png" : "jpg" });
    } catch {
      // datoteka morda ne obstaja vec - preskoci
    }
  }

  return buffers;
}

export async function readSignatureBuffer(workOrderId: string, signatureUrl: string | null): Promise<Buffer | null> {
  if (!signatureUrl) return null;
  const fileName = signatureUrl.split("/").pop() ?? "";
  try {
    return await readFile(path.join(uploadDirFor(workOrderId), fileName));
  } catch {
    return null;
  }
}

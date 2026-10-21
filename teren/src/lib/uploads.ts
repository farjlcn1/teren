import "server-only";
import { mkdir, writeFile, readFile } from "fs/promises";
import path from "path";
import sharp from "sharp";
import exifr from "exifr";

const UPLOADS_DIR = process.env.UPLOADS_DIR || "./uploads";

function workOrderDir(workOrderId: string) {
  return path.join(UPLOADS_DIR, "work-orders", workOrderId);
}

async function ensureWorkOrderDir(workOrderId: string) {
  const dir = workOrderDir(workOrderId);
  await mkdir(dir, { recursive: true });
  return dir;
}

// EXIF se prebere iz izvirnega buffera, ker ga sharp ob stiskanju ne ohrani (brez .withMetadata()).
async function readTakenAt(buffer: Buffer): Promise<Date | null> {
  try {
    const exif = await exifr.parse(buffer, ["DateTimeOriginal", "CreateDate"]);
    const date = exif?.DateTimeOriginal ?? exif?.CreateDate;
    return date instanceof Date ? date : null;
  } catch {
    return null;
  }
}

// Vrne relativno pot (shrani se v bazo), slika se stisne in omeji na max širino 1920px.
export async function savePhoto(
  workOrderId: string,
  file: File,
  index: number
): Promise<{ filePath: string; takenAt: Date | null }> {
  const dir = await ensureWorkOrderDir(workOrderId);
  const buffer = Buffer.from(await file.arrayBuffer());
  const filename = `photo-${index + 1}.jpg`;

  const takenAt = await readTakenAt(buffer);

  const resized = await sharp(buffer)
    .rotate()
    .resize({ width: 1920, withoutEnlargement: true })
    .jpeg({ quality: 80 })
    .toBuffer();

  await writeFile(path.join(dir, filename), resized);
  return { filePath: path.join("work-orders", workOrderId, filename), takenAt };
}

export async function saveSignature(workOrderId: string, file: File): Promise<string> {
  const dir = await ensureWorkOrderDir(workOrderId);
  const buffer = Buffer.from(await file.arrayBuffer());
  const filename = "signature.png";
  await writeFile(path.join(dir, filename), buffer);
  return path.join("work-orders", workOrderId, filename);
}

export async function readUploadedFile(relativePath: string): Promise<Buffer> {
  const fullPath = path.join(UPLOADS_DIR, relativePath);
  const resolved = path.resolve(fullPath);
  const uploadsRoot = path.resolve(UPLOADS_DIR);
  if (!resolved.startsWith(uploadsRoot)) {
    throw new Error("Neveljavna pot do datoteke.");
  }
  return readFile(resolved);
}

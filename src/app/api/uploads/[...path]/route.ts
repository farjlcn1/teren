import { NextRequest, NextResponse } from "next/server";
import { getSession } from "@/lib/auth/session";
import { readUploadedFile } from "@/lib/uploads";

function contentTypeFor(path: string) {
  if (path.endsWith(".png")) return "image/png";
  if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
  return "application/octet-stream";
}

export async function GET(_request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  const user = await getSession();
  if (!user) return new NextResponse("Unauthorized", { status: 401 });

  const { path: pathParts } = await params;
  const relativePath = pathParts.join("/");

  try {
    const buffer = await readUploadedFile(relativePath);
    return new NextResponse(new Uint8Array(buffer), {
      headers: {
        "Content-Type": contentTypeFor(relativePath),
        "Cache-Control": "private, max-age=31536000, immutable",
      },
    });
  } catch {
    return new NextResponse("Not found", { status: 404 });
  }
}

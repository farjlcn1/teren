"use client";

import { useRef, useImperativeHandle, forwardRef } from "react";
import SignatureCanvas from "react-signature-canvas";

export type SignaturePadHandle = {
  clear: () => void;
  getFile: () => Promise<File | null>;
  isEmpty: () => boolean;
};

const SignaturePad = forwardRef<SignaturePadHandle>((_props, ref) => {
  const sigRef = useRef<SignatureCanvas>(null);

  useImperativeHandle(ref, () => ({
    clear: () => sigRef.current?.clear(),
    isEmpty: () => sigRef.current?.isEmpty() ?? true,
    getFile: () =>
      new Promise((resolve) => {
        const canvas = sigRef.current?.getCanvas();
        if (!canvas) {
          resolve(null);
          return;
        }
        canvas.toBlob((blob) => {
          if (!blob) {
            resolve(null);
            return;
          }
          resolve(new File([blob], "podpis.png", { type: "image/png" }));
        }, "image/png");
      }),
  }));

  return (
    <div className="border border-gray-300 rounded-lg bg-white">
      <SignatureCanvas
        ref={sigRef}
        canvasProps={{ className: "w-full h-48 touch-none rounded-lg" }}
        penColor="black"
      />
    </div>
  );
});

SignaturePad.displayName = "SignaturePad";

export default SignaturePad;

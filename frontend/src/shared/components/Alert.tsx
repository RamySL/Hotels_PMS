import type { ReactNode } from "react";

type Tone = "error" | "notice";

const TONE_CLASS: Record<Tone, string> = {
  error: "bg-red-50 text-red-700",
  notice: "bg-amber-50 text-amber-800",
};

interface AlertProps {
  tone: Tone;
  children: ReactNode;
  className?: string;
}

/**
 * Message mis en évidence. Une erreur est annoncée tout de suite par les lecteurs
 * d'écran (role="alert"), une information à la première pause (role="status").
 */
export function Alert({ tone, children, className = "" }: AlertProps) {
  return (
    <p
      role={tone === "error" ? "alert" : "status"}
      className={`rounded-md px-3 py-2 text-sm ${TONE_CLASS[tone]} ${className}`}
    >
      {children}
    </p>
  );
}

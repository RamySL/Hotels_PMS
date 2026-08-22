export type ButtonVariant = "primary" | "secondary" | "danger";
export type ButtonSize = "sm" | "md";

const VARIANT_CLASS: Record<ButtonVariant, string> = {
  primary: "bg-blue-700 text-white hover:bg-blue-800",
  secondary:
    "border border-slate-300 bg-white text-slate-700 hover:bg-slate-100",
  // Action destructrice, après confirmation
  danger: "bg-red-700 text-white hover:bg-red-800",
};

const SIZE_CLASS: Record<ButtonSize, string> = {
  sm: "px-3 py-1.5 text-sm",
  md: "px-4 py-2",
};

/** Classes communes aux boutons et aux liens présentés comme des boutons. */
export function buttonClass(
  variant: ButtonVariant,
  size: ButtonSize,
  extra = "",
): string {
  return `inline-block rounded-md font-medium disabled:cursor-not-allowed disabled:opacity-60 ${VARIANT_CLASS[variant]} ${SIZE_CLASS[size]} ${extra}`;
}

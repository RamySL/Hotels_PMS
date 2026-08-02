import type { ButtonHTMLAttributes } from "react";
import {
  buttonClass,
  type ButtonSize,
  type ButtonVariant,
} from "./buttonStyles";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
}

/** Bouton de l'application ; type="button" par défaut, pour ne jamais soumettre un formulaire par mégarde. */
export function Button({
  variant = "primary",
  size = "md",
  type = "button",
  className = "",
  ...buttonProps
}: ButtonProps) {
  return (
    <button
      type={type}
      className={buttonClass(variant, size, className)}
      {...buttonProps}
    />
  );
}

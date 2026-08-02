import { Link, type LinkProps } from "react-router";
import {
  buttonClass,
  type ButtonSize,
  type ButtonVariant,
} from "./buttonStyles";

interface ButtonLinkProps extends LinkProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
}

/** Lien de navigation présenté comme un bouton (« Nouvelle chambre », « Annuler »…). */
export function ButtonLink({
  variant = "primary",
  size = "md",
  className = "",
  ...linkProps
}: ButtonLinkProps) {
  return (
    <Link className={buttonClass(variant, size, className)} {...linkProps} />
  );
}

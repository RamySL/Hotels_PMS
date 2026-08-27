import { useId, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { createClient } from "@/features/clients/api";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { ButtonLink } from "@/shared/components/ButtonLink";
import { TextField } from "@/shared/components/TextField";
import { useApiData, type ApiData } from "@/shared/hooks/useApiData";
import { useFormSubmit } from "@/shared/hooks/useFormSubmit";
import { addDays, isIsoDate, today } from "@/shared/lib/dates";
import { formatEuros } from "@/shared/lib/format";
import { createReservation } from "./api";
import { ClientPicker, type ClientChoice } from "./ClientPicker";
import type { AvailableRoom } from "./types";

/** Erreurs des champs du séjour, sous les noms de l'API. */
type StayErrors = Partial<
  Record<"checkInDate" | "checkOutDate" | "numberOfGuests", string>
>;

/** Séjour proposé à l'ouverture du formulaire. */
interface StayValues {
  checkInDate: string;
  checkOutDate: string;
  numberOfGuests: number;
  roomId: string;
}

/**
 * Création d'une réservation (/reservations/nouvelle). Depuis une case libre du planning,
 * la chambre et l'arrivée sont déjà choisies (?chambre=&arrivee=).
 */
export function ReservationFormPage() {
  const [searchParams] = useSearchParams();
  const arrival = searchParams.get("arrivee");
  const currentDay = today();
  const checkInDate =
    isIsoDate(arrival) && arrival >= currentDay ? arrival : currentDay;

  return (
    <ReservationForm
      initial={{
        checkInDate,
        checkOutDate: addDays(checkInDate, 1),
        numberOfGuests: 1,
        roomId: searchParams.get("chambre") ?? "",
      }}
    />
  );
}

function ReservationForm({ initial }: { initial: StayValues }) {
  const navigate = useNavigate();
  const [checkInDate, setCheckInDate] = useState(initial.checkInDate);
  const [checkOutDate, setCheckOutDate] = useState(initial.checkOutDate);
  const [guests, setGuests] = useState(String(initial.numberOfGuests));
  const [roomId, setRoomId] = useState(initial.roomId);
  const [client, setClient] = useState<ClientChoice>({ kind: "search" });
  const hintId = useId();

  const currentDay = today();
  const stayErrors = checkStay(checkInDate, checkOutDate, guests, currentDay);
  const datesValid = !stayErrors.checkInDate && !stayErrors.checkOutDate;
  const stayValid = datesValid && !stayErrors.numberOfGuests;
  const numberOfGuests = Number(guests);
  // Disponibilité en direct : chaque changement de dates valides relance la recherche
  const availability = useApiData<AvailableRoom[]>(
    datesValid
      ? `/rooms/availability?checkIn=${checkInDate}&checkOut=${checkOutDate}`
      : null,
  );
  // Un refus (chambre prise entre-temps) rafraîchit la liste des chambres libres
  const form = useFormSubmit({ onError: availability.reload });

  const rooms = stayValid
    ? (availability.data ?? []).filter(
        (candidate) => candidate.capacity >= numberOfGuests,
      )
    : [];
  const room = rooms.find((candidate) => candidate.roomId === roomId);
  const clientReady =
    client.kind === "chosen" ||
    (client.kind === "new" &&
      client.firstName.trim() !== "" &&
      client.lastName.trim() !== "");
  const missing = !stayValid
    ? "Corrigez le séjour pour voir les chambres libres."
    : !room
      ? "Choisissez une chambre."
      : !clientReady
        ? "Choisissez le client, ou créez-le."
        : null;

  async function save() {
    // Le bouton reste désactivé tant que la chambre ou le client manque
    if (!room || client.kind === "search") {
      return;
    }
    let clientId: string;
    if (client.kind === "chosen") {
      clientId = client.client.id;
    } else {
      const created = await createClient({
        firstName: client.firstName.trim(),
        lastName: client.lastName.trim(),
        email: null,
        phone: null,
      });
      // Le client existe désormais : si la réservation échoue, un nouvel essai le garde
      setClient({ kind: "chosen", client: created });
      clientId = created.id;
    }
    const reservation = await createReservation({
      clientId,
      roomId: room.roomId,
      checkInDate,
      checkOutDate,
      numberOfGuests,
    });
    navigate(`/reservations/${reservation.id}`);
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-900">
        Nouvelle réservation
      </h1>
      {form.error && (
        <Alert tone="error" className="mt-4">
          {form.error}
        </Alert>
      )}
      <form
        onSubmit={form.handleSubmit(save)}
        className="mt-6 max-w-2xl space-y-8"
      >
        <fieldset>
          <legend className="text-lg font-semibold text-slate-900">
            Séjour
          </legend>
          <div className="mt-3 grid gap-4 sm:grid-cols-3">
            <TextField
              label="Arrivée"
              type="date"
              required
              min={currentDay}
              value={checkInDate}
              onChange={(event) => setCheckInDate(event.target.value)}
              error={stayErrors.checkInDate ?? form.fieldErrors.checkInDate}
            />
            <TextField
              label="Départ"
              type="date"
              required
              min={isIsoDate(checkInDate) ? addDays(checkInDate, 1) : undefined}
              value={checkOutDate}
              onChange={(event) => setCheckOutDate(event.target.value)}
              error={stayErrors.checkOutDate ?? form.fieldErrors.checkOutDate}
            />
            <TextField
              label="Personnes"
              type="number"
              required
              min="1"
              step="1"
              value={guests}
              onChange={(event) => setGuests(event.target.value)}
              error={
                stayErrors.numberOfGuests ?? form.fieldErrors.numberOfGuests
              }
            />
          </div>
        </fieldset>

        <fieldset>
          <legend className="text-lg font-semibold text-slate-900">
            Chambre
          </legend>
          <div className="mt-3">
            <RoomChoice
              stayValid={stayValid}
              availability={availability}
              rooms={rooms}
              roomId={roomId}
              numberOfGuests={numberOfGuests}
              onChange={setRoomId}
            />
          </div>
        </fieldset>

        <fieldset>
          <legend className="text-lg font-semibold text-slate-900">
            Client
          </legend>
          <div className="mt-3">
            <ClientPicker
              value={client}
              onChange={setClient}
              errors={form.fieldErrors}
            />
          </div>
        </fieldset>

        <div>
          <div className="flex gap-3">
            <Button
              type="submit"
              disabled={form.saving || availability.loading || missing !== null}
              aria-describedby={missing ? hintId : undefined}
            >
              {form.saving ? "Réservation…" : "Réserver"}
            </Button>
            <ButtonLink to="/reservations" variant="secondary">
              Annuler
            </ButtonLink>
          </div>
          {missing && (
            <p id={hintId} className="mt-2 text-sm text-slate-600">
              {missing}
            </p>
          )}
        </div>
      </form>
    </>
  );
}

/**
 * Erreurs du séjour, vérifiées avant d'interroger l'API : une arrivée passée, un départ
 * qui ne la suit pas, ou un nombre de personnes qui n'est pas un entier positif.
 */
function checkStay(
  checkInDate: string,
  checkOutDate: string,
  guests: string,
  currentDay: string,
): StayErrors {
  const errors: StayErrors = {};
  if (!isIsoDate(checkInDate)) {
    errors.checkInDate = "est obligatoire";
  } else if (checkInDate < currentDay) {
    errors.checkInDate = "ne peut pas être dans le passé";
  }
  if (!isIsoDate(checkOutDate)) {
    errors.checkOutDate = "est obligatoire";
  } else if (isIsoDate(checkInDate) && checkOutDate <= checkInDate) {
    errors.checkOutDate = "doit être postérieure à la date d'arrivée";
  }
  const count = Number(guests);
  if (guests.trim() === "" || !Number.isInteger(count) || count < 1) {
    errors.numberOfGuests = "doit être un nombre entier, au moins 1";
  }
  return errors;
}

interface RoomChoiceProps {
  stayValid: boolean;
  availability: ApiData<AvailableRoom[]>;
  /** Chambres libres qui accueillent le nombre de personnes. */
  rooms: AvailableRoom[];
  roomId: string;
  numberOfGuests: number;
  onChange: (roomId: string) => void;
}

function RoomChoice({
  stayValid,
  availability,
  rooms,
  roomId,
  numberOfGuests,
  onChange,
}: RoomChoiceProps) {
  if (!stayValid) {
    return (
      <p className="text-sm text-slate-600">
        Les chambres libres s'affichent pour un séjour valide.
      </p>
    );
  }
  if (availability.error) {
    return <Alert tone="error">{availability.error.message}</Alert>;
  }
  if (availability.data === undefined) {
    return (
      <p className="text-sm text-slate-500">Recherche des chambres libres…</p>
    );
  }
  if (rooms.length === 0) {
    return (
      <p className="text-sm text-slate-600">
        Aucune chambre libre ne peut accueillir {numberOfGuests}{" "}
        {numberOfGuests > 1 ? "personnes" : "personne"} à ces dates.
      </p>
    );
  }
  return (
    <ul
      aria-busy={availability.loading}
      className={`space-y-2 ${availability.loading ? "opacity-60" : ""}`}
    >
      {rooms.map((candidate) => (
        <li key={candidate.roomId}>
          <label className="flex cursor-pointer items-center gap-3 rounded-md border border-slate-200 bg-white px-3 py-2 has-checked:border-blue-600 has-checked:bg-blue-50">
            <input
              type="radio"
              name="roomId"
              value={candidate.roomId}
              checked={candidate.roomId === roomId}
              onChange={() => onChange(candidate.roomId)}
            />
            <span className="font-medium text-slate-900">
              Chambre {candidate.roomNumber}
            </span>{" "}
            <span className="text-sm text-slate-600">
              {candidate.roomTypeName} · {candidate.capacity} pers.
            </span>{" "}
            <span className="ml-auto text-right">
              <span className="block font-medium text-slate-900">
                {formatEuros(candidate.totalPrice)}
              </span>{" "}
              <span className="block text-xs text-slate-500">
                {candidate.nights} {candidate.nights > 1 ? "nuits" : "nuit"} ×{" "}
                {formatEuros(candidate.pricePerNight)}
              </span>
            </span>
          </label>
        </li>
      ))}
    </ul>
  );
}

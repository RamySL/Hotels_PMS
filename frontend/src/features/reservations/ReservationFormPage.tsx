import { useId, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router";
import { createClient } from "@/features/clients/api";
import type { Room } from "@/features/rooms/types";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { ButtonLink } from "@/shared/components/ButtonLink";
import { TextField } from "@/shared/components/TextField";
import { useApiData, type ApiData } from "@/shared/hooks/useApiData";
import { useFormSubmit } from "@/shared/hooks/useFormSubmit";
import { addDays, daysBetween, isIsoDate, today } from "@/shared/lib/dates";
import { formatEuros } from "@/shared/lib/format";
import { createReservation, updateReservation } from "./api";
import { ClientPicker, type ClientChoice } from "./ClientPicker";
import type { AvailableRoom, Reservation } from "./types";

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

/** Réservation modifiée, avec sa chambre : le type donne la capacité et le prix par nuit. */
interface Editing {
  reservation: Reservation;
  room: Room;
}

/**
 * Création (/reservations/nouvelle) ou modification (/reservations/:id/modifier) d'une
 * réservation.
 */
export function ReservationFormPage() {
  const { id } = useParams();
  return id ? <EditReservation id={id} /> : <NewReservation />;
}

/** Depuis une case libre du planning, la chambre et l'arrivée sont déjà choisies (?chambre=&arrivee=). */
function NewReservation() {
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

function EditReservation({ id }: { id: string }) {
  const reservation = useApiData<Reservation>(`/reservations/${id}`);
  const rooms = useApiData<Room[]>("/rooms");

  const error = reservation.error ?? rooms.error;
  if (error) {
    return error.status === 404 ? (
      <p className="text-slate-600">Cette réservation n'existe pas.</p>
    ) : (
      <Alert tone="error">{error.message}</Alert>
    );
  }
  if (reservation.data === undefined || rooms.data === undefined) {
    return <p className="text-slate-500">Chargement…</p>;
  }
  const stay = reservation.data;
  if (stay.status === "CHECKED_OUT" || stay.status === "CANCELLED") {
    return (
      <>
        <p className="text-slate-600">
          Une réservation terminée ou annulée n'est plus modifiable.
        </p>
        <ButtonLink
          to={`/reservations/${id}`}
          variant="secondary"
          className="mt-4"
        >
          Retour à la réservation
        </ButtonLink>
      </>
    );
  }
  const room = rooms.data.find((candidate) => candidate.id === stay.room.id);
  if (!room) {
    return (
      <p className="text-slate-600">La chambre de ce séjour n'existe plus.</p>
    );
  }
  return (
    <ReservationForm
      initial={{
        checkInDate: stay.checkInDate,
        checkOutDate: stay.checkOutDate,
        numberOfGuests: stay.numberOfGuests,
        roomId: stay.room.id,
      }}
      editing={{ reservation: stay, room }}
    />
  );
}

interface ReservationFormProps {
  initial: StayValues;
  /** Absent pour une création. */
  editing?: Editing;
}

function ReservationForm({ initial, editing }: ReservationFormProps) {
  const navigate = useNavigate();
  const [checkInDate, setCheckInDate] = useState(initial.checkInDate);
  const [checkOutDate, setCheckOutDate] = useState(initial.checkOutDate);
  const [guests, setGuests] = useState(String(initial.numberOfGuests));
  const [roomId, setRoomId] = useState(initial.roomId);
  const [client, setClient] = useState<ClientChoice>({ kind: "search" });
  const hintId = useId();

  const currentDay = today();
  // Pendant le séjour, seule la date de départ peut changer
  const duringStay = editing?.reservation.status === "CHECKED_IN";
  const savedCheckIn = editing ? initial.checkInDate : undefined;
  const stayErrors = checkStay(
    { checkInDate, checkOutDate, guests },
    { currentDay, savedCheckIn, duringStay },
  );
  const datesValid = !stayErrors.checkInDate && !stayErrors.checkOutDate;
  const stayValid = datesValid && !stayErrors.numberOfGuests;
  const numberOfGuests = Number(guests);
  // Disponibilité en direct, tant que la chambre peut changer
  const availability = useApiData<AvailableRoom[]>(
    datesValid && !duringStay
      ? `/rooms/availability?checkIn=${checkInDate}&checkOut=${checkOutDate}`
      : null,
  );
  // Un refus (chambre prise entre-temps) rafraîchit la liste des chambres libres
  const form = useFormSubmit({ onError: availability.reload });

  const offers = stayValid
    ? roomOffers(
        duringStay ? [] : (availability.data ?? []),
        editing && offerOf(editing.room, checkInDate, checkOutDate),
      ).filter((offer) => offer.capacity >= numberOfGuests)
    : [];
  const room = offers.find((offer) => offer.roomId === roomId);
  const clientReady =
    editing !== undefined ||
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
    if (!room) {
      return;
    }
    const stay = {
      roomId: room.roomId,
      checkInDate,
      checkOutDate,
      numberOfGuests,
    };
    if (editing) {
      const { id, client: owner } = editing.reservation;
      // Le client d'une réservation ne change pas : le backend ignore clientId ici
      await updateReservation(id, { clientId: owner.id, ...stay });
      navigate(`/reservations/${id}`);
      return;
    }
    if (client.kind === "search") {
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
    const reservation = await createReservation({ clientId, ...stay });
    navigate(`/reservations/${reservation.id}`);
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-900">
        {editing ? "Modifier la réservation" : "Nouvelle réservation"}
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
          {duringStay && (
            <p className="mt-1 text-sm text-slate-600">
              Pendant le séjour, seule la date de départ peut changer :
              prolongation ou départ anticipé.
            </p>
          )}
          <div className="mt-3 grid gap-4 sm:grid-cols-3">
            <TextField
              label="Arrivée"
              type="date"
              required
              disabled={duringStay}
              min={earliest(currentDay, savedCheckIn)}
              value={checkInDate}
              onChange={(event) => setCheckInDate(event.target.value)}
              error={stayErrors.checkInDate ?? form.fieldErrors.checkInDate}
            />
            <TextField
              label="Départ"
              type="date"
              required
              min={
                isIsoDate(checkInDate)
                  ? latest(
                      addDays(checkInDate, 1),
                      duringStay ? currentDay : undefined,
                    )
                  : undefined
              }
              value={checkOutDate}
              onChange={(event) => setCheckOutDate(event.target.value)}
              error={stayErrors.checkOutDate ?? form.fieldErrors.checkOutDate}
            />
            <TextField
              label="Personnes"
              type="number"
              required
              disabled={duringStay}
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
              locked={duringStay}
              offers={offers}
              roomId={roomId}
              currentRoomId={editing?.room.id}
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
            {editing ? (
              <p className="font-medium text-slate-900">
                {editing.reservation.client.firstName}{" "}
                {editing.reservation.client.lastName}
              </p>
            ) : (
              <ClientPicker
                value={client}
                onChange={setClient}
                errors={form.fieldErrors}
              />
            )}
          </div>
        </fieldset>

        <div>
          <div className="flex gap-3">
            <Button
              type="submit"
              disabled={form.saving || availability.loading || missing !== null}
              aria-describedby={missing ? hintId : undefined}
            >
              {submitLabel(editing !== undefined, form.saving)}
            </Button>
            <ButtonLink
              to={
                editing
                  ? `/reservations/${editing.reservation.id}`
                  : "/reservations"
              }
              variant="secondary"
            >
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

function submitLabel(editing: boolean, saving: boolean): string {
  if (editing) {
    return saving ? "Enregistrement…" : "Enregistrer";
  }
  return saving ? "Réservation…" : "Réserver";
}

interface StayRules {
  currentDay: string;
  /** Arrivée déjà enregistrée : passée, elle reste admise tant qu'elle ne change pas. */
  savedCheckIn?: string;
  /** Pendant le séjour, le départ ne peut pas non plus être dans le passé. */
  duringStay: boolean;
}

/**
 * Erreurs du séjour, vérifiées avant d'interroger l'API : une arrivée passée, un départ
 * qui ne la suit pas, ou un nombre de personnes qui n'est pas un entier positif.
 */
function checkStay(
  {
    checkInDate,
    checkOutDate,
    guests,
  }: { checkInDate: string; checkOutDate: string; guests: string },
  { currentDay, savedCheckIn, duringStay }: StayRules,
): StayErrors {
  const errors: StayErrors = {};
  if (!isIsoDate(checkInDate)) {
    errors.checkInDate = "est obligatoire";
  } else if (checkInDate < currentDay && checkInDate !== savedCheckIn) {
    errors.checkInDate = "ne peut pas être dans le passé";
  }
  if (!isIsoDate(checkOutDate)) {
    errors.checkOutDate = "est obligatoire";
  } else if (duringStay && checkOutDate < currentDay) {
    errors.checkOutDate = "ne peut pas être dans le passé";
  } else if (isIsoDate(checkInDate) && checkOutDate <= checkInDate) {
    errors.checkOutDate = "doit être postérieure à la date d'arrivée";
  }
  const count = Number(guests);
  if (guests.trim() === "" || !Number.isInteger(count) || count < 1) {
    errors.numberOfGuests = "doit être un nombre entier, au moins 1";
  }
  return errors;
}

/**
 * Chambres à proposer : les chambres libres, plus la chambre actuelle d'une réservation
 * modifiée. La disponibilité la compte occupée par cette réservation même, alors que le
 * backend l'exclut de sa vérification ; s'il reste un conflit, c'est lui qui le dira.
 */
function roomOffers(
  free: AvailableRoom[],
  current: AvailableRoom | undefined,
): AvailableRoom[] {
  if (!current || free.some((offer) => offer.roomId === current.roomId)) {
    return free;
  }
  return [...free, current].sort(
    (a, b) => a.floor - b.floor || a.roomNumber.localeCompare(b.roomNumber),
  );
}

/** La chambre au tarif de son type, comme le calcule le backend (ADR-004). */
function offerOf(
  room: Room,
  checkInDate: string,
  checkOutDate: string,
): AvailableRoom {
  const nights = daysBetween(checkInDate, checkOutDate);
  return {
    roomId: room.id,
    roomNumber: room.roomNumber,
    floor: room.floor,
    roomTypeName: room.roomType.name,
    capacity: room.roomType.capacity,
    pricePerNight: room.roomType.basePrice,
    nights,
    totalPrice: room.roomType.basePrice * nights,
  };
}

/** La plus ancienne des deux dates, au format de l'API ; other peut manquer. */
function earliest(date: string, other: string | undefined): string {
  return other !== undefined && other < date ? other : date;
}

/** La plus récente des deux dates, au format de l'API ; other peut manquer. */
function latest(date: string, other: string | undefined): string {
  return other !== undefined && other > date ? other : date;
}

interface RoomChoiceProps {
  stayValid: boolean;
  availability: ApiData<AvailableRoom[]>;
  /** Pendant le séjour, la chambre ne change pas : rien n'est cherché. */
  locked: boolean;
  /** Chambres proposées, qui accueillent le nombre de personnes. */
  offers: AvailableRoom[];
  roomId: string;
  currentRoomId: string | undefined;
  numberOfGuests: number;
  onChange: (roomId: string) => void;
}

function RoomChoice({
  stayValid,
  availability,
  locked,
  offers,
  roomId,
  currentRoomId,
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
  if (!locked && availability.error) {
    return <Alert tone="error">{availability.error.message}</Alert>;
  }
  if (!locked && availability.data === undefined) {
    return (
      <p className="text-sm text-slate-500">Recherche des chambres libres…</p>
    );
  }
  if (offers.length === 0) {
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
      {offers.map((offer) => (
        <li key={offer.roomId}>
          <label className="flex cursor-pointer items-center gap-3 rounded-md border border-slate-200 bg-white px-3 py-2 has-checked:border-blue-600 has-checked:bg-blue-50 has-disabled:cursor-default">
            <input
              type="radio"
              name="roomId"
              value={offer.roomId}
              checked={offer.roomId === roomId}
              disabled={locked}
              onChange={() => onChange(offer.roomId)}
            />
            <span className="font-medium text-slate-900">
              Chambre {offer.roomNumber}
              {offer.roomId === currentRoomId && " (actuelle)"}
            </span>{" "}
            <span className="text-sm text-slate-600">
              {offer.roomTypeName} · {offer.capacity} pers.
            </span>{" "}
            <span className="ml-auto text-right">
              <span className="block font-medium text-slate-900">
                {formatEuros(offer.totalPrice)}
              </span>{" "}
              <span className="block text-xs text-slate-500">
                {offer.nights} {offer.nights > 1 ? "nuits" : "nuit"} ×{" "}
                {formatEuros(offer.pricePerNight)}
              </span>
            </span>
          </label>
        </li>
      ))}
    </ul>
  );
}

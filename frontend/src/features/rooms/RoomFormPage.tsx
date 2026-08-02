import { useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { ButtonLink } from "@/shared/components/ButtonLink";
import { SelectField } from "@/shared/components/SelectField";
import { TextField } from "@/shared/components/TextField";
import { useApiData } from "@/shared/hooks/useApiData";
import { ApiError } from "@/shared/lib/apiClient";
import { toNumberOrNull } from "@/shared/lib/form";
import { formatEuros } from "@/shared/lib/format";
import { createRoom, updateRoom } from "./api";
import type { Room, RoomRequest, RoomType } from "./types";

/** Création (/chambres/nouvelle) ou modification (/chambres/:id/modifier) d'une chambre. */
export function RoomFormPage() {
  const { id } = useParams();
  return id ? <EditRoom id={id} /> : <NewRoom />;
}

function NewRoom() {
  const types = useApiData<RoomType[]>("/room-types");

  if (types.error) {
    return <Alert tone="error">{types.error.message}</Alert>;
  }
  if (types.data === undefined) {
    return <p className="text-slate-500">Chargement…</p>;
  }
  if (types.data.length === 0) {
    return (
      <>
        <h1 className="text-2xl font-bold text-slate-900">Nouvelle chambre</h1>
        <p className="mt-2 text-slate-600">
          Il n'existe encore aucun type de chambre : créez-en un avant d'ajouter
          des chambres.
        </p>
        <ButtonLink to="/chambres/types/nouveau" className="mt-4">
          Créer un type de chambre
        </ButtonLink>
      </>
    );
  }
  return (
    <RoomForm
      title="Nouvelle chambre"
      roomTypes={types.data}
      onSave={createRoom}
    />
  );
}

function EditRoom({ id }: { id: string }) {
  const types = useApiData<RoomType[]>("/room-types");
  const rooms = useApiData<Room[]>("/rooms");

  const error = types.error ?? rooms.error;
  if (error) {
    return <Alert tone="error">{error.message}</Alert>;
  }
  if (types.data === undefined || rooms.data === undefined) {
    return <p className="text-slate-500">Chargement…</p>;
  }
  const room = rooms.data.find((candidate) => candidate.id === id);
  if (!room) {
    return <p className="text-slate-600">Cette chambre n'existe pas.</p>;
  }
  return (
    <RoomForm
      key={room.id}
      title={`Chambre ${room.roomNumber}`}
      initial={room}
      roomTypes={types.data}
      onSave={(request) => updateRoom(room.id, request)}
    />
  );
}

interface RoomFormProps {
  title: string;
  initial?: Room;
  roomTypes: RoomType[];
  onSave: (request: RoomRequest) => Promise<unknown>;
}

function RoomForm({ title, initial, roomTypes, onSave }: RoomFormProps) {
  const navigate = useNavigate();
  const [roomNumber, setRoomNumber] = useState(initial?.roomNumber ?? "");
  const [floor, setFloor] = useState(initial ? String(initial.floor) : "");
  const [roomTypeId, setRoomTypeId] = useState(initial?.roomType.id ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const typeOptions = [
    { value: "", label: "Choisir un type" },
    ...roomTypes.map((type) => ({
      value: type.id,
      label: `${type.name} — ${formatEuros(type.basePrice)} — ${type.capacity} pers.`,
    })),
  ];

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setFieldErrors({});
    try {
      await onSave({
        roomNumber: roomNumber.trim(),
        floor: toNumberOrNull(floor),
        roomTypeId: roomTypeId || null,
      });
      navigate("/chambres");
    } catch (e) {
      setError(
        e instanceof ApiError ? e.message : "L'enregistrement a échoué.",
      );
      setFieldErrors(e instanceof ApiError ? e.errors : {});
      setSaving(false);
    }
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
      {error && (
        <Alert tone="error" className="mt-4">
          {error}
        </Alert>
      )}
      <form onSubmit={handleSubmit} className="mt-6 max-w-md space-y-4">
        <TextField
          label="Numéro"
          required
          maxLength={10}
          value={roomNumber}
          onChange={(event) => setRoomNumber(event.target.value)}
          error={fieldErrors.roomNumber}
        />
        <TextField
          label="Étage"
          type="number"
          min="0"
          step="1"
          required
          value={floor}
          onChange={(event) => setFloor(event.target.value)}
          error={fieldErrors.floor}
        />
        <SelectField
          label="Type"
          required
          options={typeOptions}
          value={roomTypeId}
          onChange={(event) => setRoomTypeId(event.target.value)}
          error={fieldErrors.roomTypeId}
        />
        <div className="flex gap-3">
          <Button type="submit" disabled={saving}>
            {saving ? "Enregistrement…" : "Enregistrer"}
          </Button>
          <ButtonLink to="/chambres" variant="secondary">
            Annuler
          </ButtonLink>
        </div>
      </form>
    </>
  );
}

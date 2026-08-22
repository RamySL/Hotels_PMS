import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { ButtonLink } from "@/shared/components/ButtonLink";
import { TextField } from "@/shared/components/TextField";
import { useApiData } from "@/shared/hooks/useApiData";
import { useFormSubmit } from "@/shared/hooks/useFormSubmit";
import { toNumberOrNull, toTextOrNull } from "@/shared/lib/form";
import { createRoomType, updateRoomType } from "./api";
import type { RoomType, RoomTypeRequest } from "./types";

/** Création (/chambres/types/nouveau) ou modification (/chambres/types/:id/modifier) d'un type. */
export function RoomTypeFormPage() {
  const { id } = useParams();
  return id ? (
    <EditRoomType id={id} />
  ) : (
    <RoomTypeForm title="Nouveau type de chambre" onSave={createRoomType} />
  );
}

function EditRoomType({ id }: { id: string }) {
  const types = useApiData<RoomType[]>("/room-types");

  if (types.error) {
    return <Alert tone="error">{types.error.message}</Alert>;
  }
  if (types.data === undefined) {
    return <p className="text-slate-500">Chargement…</p>;
  }
  const roomType = types.data.find((type) => type.id === id);
  if (!roomType) {
    return <p className="text-slate-600">Ce type de chambre n'existe pas.</p>;
  }
  return (
    <RoomTypeForm
      key={roomType.id}
      title={`Type « ${roomType.name} »`}
      initial={roomType}
      onSave={(request) => updateRoomType(roomType.id, request)}
    />
  );
}

interface RoomTypeFormProps {
  title: string;
  initial?: RoomType;
  onSave: (request: RoomTypeRequest) => Promise<unknown>;
}

function RoomTypeForm({ title, initial, onSave }: RoomTypeFormProps) {
  const navigate = useNavigate();
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [price, setPrice] = useState(initial ? String(initial.basePrice) : "");
  const [capacity, setCapacity] = useState(
    initial ? String(initial.capacity) : "",
  );
  const form = useFormSubmit();

  async function save() {
    await onSave({
      name: name.trim(),
      description: toTextOrNull(description),
      basePrice: toNumberOrNull(price),
      capacity: toNumberOrNull(capacity),
    });
    navigate("/chambres/types");
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
      {form.error && (
        <Alert tone="error" className="mt-4">
          {form.error}
        </Alert>
      )}
      <form
        onSubmit={form.handleSubmit(save)}
        className="mt-6 max-w-md space-y-4"
      >
        <TextField
          label="Nom"
          required
          maxLength={100}
          value={name}
          onChange={(event) => setName(event.target.value)}
          error={form.fieldErrors.name}
        />
        <TextField
          label="Description"
          maxLength={1000}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          error={form.fieldErrors.description}
        />
        <TextField
          label="Prix par nuit (€)"
          type="number"
          min="0.01"
          step="0.01"
          required
          value={price}
          onChange={(event) => setPrice(event.target.value)}
          error={form.fieldErrors.basePrice}
        />
        <TextField
          label="Capacité (personnes)"
          type="number"
          min="1"
          step="1"
          required
          value={capacity}
          onChange={(event) => setCapacity(event.target.value)}
          error={form.fieldErrors.capacity}
        />
        <div className="flex gap-3">
          <Button type="submit" disabled={form.saving}>
            {form.saving ? "Enregistrement…" : "Enregistrer"}
          </Button>
          <ButtonLink to="/chambres/types" variant="secondary">
            Annuler
          </ButtonLink>
        </div>
      </form>
    </>
  );
}

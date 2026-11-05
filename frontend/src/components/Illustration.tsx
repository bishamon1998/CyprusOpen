import { Globe2, MapPin, Users } from "lucide-react";

const icons = {
  people: [Users, Globe2],
  travel: [Globe2, MapPin],
} as const;

export type IllustrationKind = keyof typeof icons;

export function Illustration({ kind }: { kind: IllustrationKind }) {
  const [MainIcon, SmallIcon] = icons[kind];
  return (
    <div className={`illustration ${kind}`} aria-hidden="true">
      <span className="orbit orbit-one" />
      <span className="orbit orbit-two" />
      <span className="illustration-line" />
      <span className="illustration-dot dot-one" />
      <span className="illustration-dot dot-two" />
      <span className="icon-tile main-tile">
        <MainIcon size={43} strokeWidth={1.4} />
      </span>
      <span className="icon-tile small-tile">
        <SmallIcon size={19} strokeWidth={1.6} />
      </span>
      <span className="illustration-spark">+</span>
    </div>
  );
}

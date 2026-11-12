import { useState } from "react";
import { useI18n } from "../i18n";
import { section } from "../navigation";
import { SoftTile } from "./icons/SoftTile";
import { Mouflon } from "./Mouflon";

/** The page heading; for a section page also its tile, one line on what it offers and its mouflon. */
export function PageTitle({ title, path }: { title: string; path?: string }) {
  const { t } = useI18n();
  const item = path === undefined ? undefined : section(path);
  // A wiggle starts on hover and always plays to the end, so moving the mouse away never cuts it off.
  const [wiggling, setWiggling] = useState(false);
  return (
    <div className={"page-title" + (item ? " with-section" : "")}>
      {item && <SoftTile icon={item.icon} size={48} />}
      <div className="page-title-text">
        <h1>{title}</h1>
        {item && <p>{t(item.lead)}</p>}
      </div>
      {item && (
        <span
          className={"page-mouflon-wrap" + (wiggling ? " wiggling" : "")}
          onMouseEnter={() => setWiggling(true)}
          onAnimationEnd={(event) => {
            if (event.animationName === "mouflon-wiggle") setWiggling(false);
          }}
        >
          <Mouflon scene={item.mouflon} className="page-mouflon" />
        </span>
      )}
    </div>
  );
}

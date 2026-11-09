import { Link } from "react-router-dom";
import { SoftTile } from "../components/icons/SoftTile";
import { Mouflon } from "../components/Mouflon";
import { PageTitle } from "../components/PageTitle";
import { useI18n } from "../i18n";
import { sections } from "../navigation";

export default function NotFound() {
  const { t } = useI18n();
  return (
    <section className="not-found">
      <Mouflon scene="not-found" className="not-found-mouflon" />
      <PageTitle title={t("notFound")} />
      <p>{t("notFoundText")}</p>
      <ul>
        {sections.map((item) => (
          <li key={item.path}>
            <Link to={item.path}>
              <SoftTile icon={item.icon} size={30} />
              {t(item.label)}
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

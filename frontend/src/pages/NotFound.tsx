import { Link } from "react-router-dom";
import { PageTitle } from "../components/PageTitle";
import { useI18n } from "../i18n";

export default function NotFound() {
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("notFound")} />
      <Link className="back-link" to="/">
        {t("back")}
      </Link>
    </>
  );
}

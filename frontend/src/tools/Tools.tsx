import { PageTitle } from "../components/PageTitle";
import { useI18n } from "../i18n";
import { Holidays } from "./Holidays";
import { Salary } from "./Salary";
import "./tools.css";

/** Reference tools computed in the browser: public holidays and a net salary calculator. */
export default function Tools() {
  const { t } = useI18n();
  return (
    <>
      <PageTitle title={t("tools")} path="/tools" />
      <div className="tools-grid">
        <Holidays />
        <Salary />
      </div>
    </>
  );
}

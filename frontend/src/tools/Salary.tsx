import { useId, useState } from "react";
import { InfoPopover } from "../components/InfoPopover";
import { SegmentedControl } from "../components/SegmentedControl";
import { SourceLink } from "../components/SourceLink";
import { useI18n } from "../i18n";
import { socialInsuranceUrl, taxDepartmentUrl } from "../sources";
import { netSalary, newResidentExemption, salaryRules } from "./salaryRules";

type Period = "month" | "year";

/** Take-home pay of an employee from a gross monthly or yearly salary. */
export function Salary() {
  const { t, language } = useI18n();
  const input = useId();
  const [amount, setAmount] = useState("2500");
  const [period, setPeriod] = useState<Period>("month");
  const [payments, setPayments] = useState(12);
  const [newResident, setNewResident] = useState(false);
  const value = Number(amount.replace(",", "."));
  const valid = amount.trim() !== "" && Number.isFinite(value) && value >= 0 && value <= 10_000_000;
  const yearly = valid ? (period === "month" ? value * payments : value) : 0;
  const result = netSalary(yearly, newResident);
  const money = (euros: number, digits = 0) =>
    new Intl.NumberFormat(language, {
      style: "currency",
      currency: "EUR",
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    }).format(euros);
  const percent = (share: number) =>
    new Intl.NumberFormat(language, { style: "percent", maximumFractionDigits: 2 }).format(share);
  const rows = [
    { label: t("grossSalary"), value: result.gross },
    { label: t("socialInsurance"), value: -result.socialInsurance },
    { label: t("gesy"), value: -result.gesy },
    { label: t("incomeTax"), value: -result.incomeTax },
  ];

  return (
    <article className="dataset-card tool-card" aria-labelledby="salary-title">
      <div className="card-heading">
        <h3 id="salary-title">{t("salary")}</h3>
      </div>
      <p className="card-description">{t("salaryDescription", { year: salaryRules.year })}</p>
      <div className="salary-form">
        <label htmlFor={input}>{t("grossAmount")}</label>
        <div className="salary-input">
          <span aria-hidden="true">€</span>
          <input
            id={input}
            type="number"
            inputMode="decimal"
            min={0}
            step={50}
            value={amount}
            aria-invalid={!valid}
            onChange={(event) => setAmount(event.target.value)}
          />
        </div>
        <div className="salary-options">
          <SegmentedControl
            label={t("salaryPeriod")}
            value={period}
            options={[
              { value: "month", label: t("perMonth") },
              { value: "year", label: t("perYear") },
            ]}
            onChange={setPeriod}
          />
          <SegmentedControl
            label={t("salaryPayments")}
            value={String(payments)}
            options={[
              { value: "12", label: t("payments12") },
              { value: "13", label: t("payments13") },
            ]}
            onChange={(next) => setPayments(Number(next))}
          />
        </div>
        <label className="salary-check">
          <input
            type="checkbox"
            checked={newResident}
            onChange={(event) => setNewResident(event.target.checked)}
            aria-describedby={newResident ? `${input}-exemption` : undefined}
          />
          <span>{t("newResident")}</span>
        </label>
        {newResident && (
          <p className="salary-hint" id={`${input}-exemption`}>
            {newResidentExemption(result.gross) === "half"
              ? t("newResidentHalf", { amount: money(result.exemption), threshold: money(salaryRules.newResidentHalfAbove) })
              : t("newResidentFifth", {
                  amount: money(result.exemption),
                  threshold: money(salaryRules.newResidentHalfAbove),
                  cap: money(salaryRules.newResidentFifthCap),
                })}
          </p>
        )}
      </div>
      <div className="salary-result" aria-live="polite">
        <p>
          <span>{t("netPerPayment")}</span>
          <strong>{money(result.net / payments)}</strong>
        </p>
        <p>
          <span>{t("netPerYear")}</span>
          <strong>{money(result.net)}</strong>
        </p>
      </div>
      <table className="salary-table">
        <caption>{t("salaryBreakdown")}</caption>
        <thead>
          <tr>
            <th scope="col">{t("salaryItem")}</th>
            <th scope="col">{t("perYear")}</th>
            <th scope="col">{t("perPayment")}</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.label}>
              <th scope="row">{row.label}</th>
              <td>{money(row.value, 2)}</td>
              <td>{money(row.value / payments, 2)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <th scope="row">{t("netSalary")}</th>
            <td>{money(result.net, 2)}</td>
            <td>{money(result.net / payments, 2)}</td>
          </tr>
        </tfoot>
      </table>
      <p className="tool-card-summary">
        {t("salarySummary", {
          share: percent(result.gross > 0 ? (result.gross - result.net) / result.gross : 0),
        })}
      </p>
      <div className="info-bar">
        <InfoPopover>
          <p className="info-popover-lead">{t("salaryEstimate")}</p>
          <p>
            {t("salaryRatesNote", {
              insurance: percent(salaryRules.socialInsuranceRate),
              ceiling: money(salaryRules.socialInsuranceCeiling),
              gesy: percent(salaryRules.gesyRate),
              free: money(salaryRules.taxBands[1].from),
            })}
          </p>
          <p>{t("newResidentConditions")}</p>
          <p>{t("salaryExcludes")}</p>
          <SourceLink href={taxDepartmentUrl}>{t("taxDepartment")}</SourceLink>
          <SourceLink href={socialInsuranceUrl}>{t("socialInsuranceServices")}</SourceLink>
        </InfoPopover>
      </div>
    </article>
  );
}

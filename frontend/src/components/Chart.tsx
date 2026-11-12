import { useEffect, useRef } from "react";
import { BarChart, HeatmapChart, LineChart } from "echarts/charts";
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
  VisualMapComponent,
} from "echarts/components";
import * as echarts from "echarts/core";
import { SVGRenderer } from "echarts/renderers";

echarts.use([
  BarChart,
  HeatmapChart,
  LineChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  VisualMapComponent,
  SVGRenderer,
]);

export type ChartOption = echarts.EChartsCoreOption;

/** Brand palette shared by every chart: blue accent first, then graphite tones. */
export const chartColors = ["#4263eb", "#9aa9f5", "#191c24", "#8a909d", "#c9cedb"];

const baseOption: ChartOption = {
  color: chartColors,
  textStyle: { fontFamily: "'Manrope Variable', ui-sans-serif, system-ui, sans-serif", color: "#191c24" },
  animationDuration: 300,
};

/**
 * Responsive ECharts canvas. The caller supplies the option; this component owns
 * initialization, resizing and disposal. `label` is the accessible description.
 */
export default function Chart({
  option,
  height,
  label,
}: {
  option: ChartOption;
  height: number;
  label: string;
}) {
  const element = useRef<HTMLDivElement>(null);
  const chart = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    if (!element.current) return;
    const instance = echarts.init(element.current, null, { renderer: "svg" });
    chart.current = instance;
    const observer = new ResizeObserver(() => instance.resize());
    observer.observe(element.current);
    return () => {
      observer.disconnect();
      instance.dispose();
      chart.current = null;
    };
  }, []);

  useEffect(() => {
    chart.current?.setOption({ ...baseOption, ...option }, true);
  }, [option]);

  return (
    <div
      ref={element}
      className="chart"
      style={{ height }}
      role="img"
      aria-label={label}
    />
  );
}

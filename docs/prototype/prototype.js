const translations = {
  en: {
    monthlyBreakdown: 'Monthly breakdown · layout only',
    skip: 'Skip to content', draft: 'INTERFACE DRAFT · No live data', previewState: 'Preview state', layout: 'Layout', loading: 'Loading', empty: 'No data', error: 'Error', stale: 'Out of date',
    statistics: 'Statistics', parking: 'Parking', pharmacies: 'Pharmacies', about: 'About & sources', publicData: 'Public data, a clearer picture', statisticsTitle: 'A closer look at Cyprus.', statisticsIntro: 'Where people come from, who visits, and how the island changes. Two questions to start with.',
    migration: 'People & migration', immigrationTitle: 'Where do people moving to Cyprus come from?', immigrationDescription: 'Annual immigration flows by country of previous residence. The exact source and available country breakdown still need verification.', year: 'Year', awaitingMetadata: 'Awaiting source metadata', immigrationUnit: 'Unit: people · definition pending verification', immigrationSource: 'Source not connected. Coverage, definition and last successful sync will appear here.',
    tourism: 'Travel & seasons', tourismTitle: 'Who visits Cyprus most often?', tourismDescription: 'Tourist arrivals by country of usual residence, with a seasonal breakdown. Arrivals count trips, not unique visitors.', season: 'Season', tourismSource: 'CYSTAT is a source candidate. Monthly data, reuse terms and historical coverage are still being checked.',
    chartPlaceholder: 'Chart layout only · bars do not represent values.', loadingTitle: 'Loading the dataset', loadingBody: 'The chart and filters will appear when the request finishes.', emptyTitle: 'No published data for this selection', emptyBody: 'Keep the source and period visible. Missing values are never shown as zero.', errorTitle: 'We could not load this dataset', errorBody: 'Keep the selected filters. In the working app, a retry action will repeat the request to our backend.', staleBody: 'The last saved dataset would stay visible here, with the last successful sync and a freshness warning.',
    aroundYou: 'Around the city', parkingTitle: 'Find a place to park.', parkingIntro: 'A map when you need an overview. A list when you want the details. Always with the time of the latest report.', list: 'List', map: 'Map', parkingPlaceholder: 'Parking name', parkingDetails: 'Verified address and latest report will appear here.', unknown: 'No report', mapArea: 'Map area', mapPending: 'Verified locations will appear here.', noCoordinates: 'No map tiles or invented coordinates are used in this draft.', parkingSource: 'Live parking source not connected. No free-space counts are shown.', parkingUnavailable: 'Availability is not connected. No report does not mean full.', parkingStale: 'Old report: keep the last permitted count with its timestamp. Hide the count once it is too old.', parkingLoading: 'Loading the latest saved report…', parkingEmpty: 'No participating parking locations are available.', parkingError: 'The report could not be loaded. Keep the list available if it has a valid saved snapshot.',
    comingSoon: 'Coming later', pharmaciesIntro: 'On-duty pharmacies, once a reliable official duty schedule is available.', pharmaciesPending: 'This feature is not available yet. No pharmacy is listed as currently on duty.', transparency: 'Where the numbers come from', aboutTitle: 'Sources you can check.', aboutIntro: 'Every dataset will carry its original source, definition, coverage and update information.', publisher: 'Source candidates', definitions: 'Definitions', definitionsValue: 'Immigration flows and tourist arrivals are different measures. Country of residence is not citizenship.', license: 'Reuse & licence', licensePending: 'Pending verification for each exact resource.', lastSync: 'Last successful sync', notSynced: 'Not synced. This is an interface draft.', scope: 'Geographic coverage', scopeValue: 'Show the scope stated by each dataset. Do not assume coverage of the whole island.', footer: 'Open information. Clear sources.', navigationLabel: 'Main navigation', languageLabel: 'Language', parkingViewLabel: 'Parking view'
  },
  ru: {
    monthlyBreakdown: 'Помесячная разбивка · только компоновка',
    skip: 'Перейти к содержимому', draft: 'ЭСКИЗ ИНТЕРФЕЙСА · Без живых данных', previewState: 'Состояние', layout: 'Компоновка', loading: 'Загрузка', empty: 'Нет данных', error: 'Ошибка', stale: 'Устаревшие данные',
    statistics: 'Статистика', parking: 'Парковки', pharmacies: 'Аптеки', about: 'О проекте и данных', publicData: 'Открытые данные, понятная картина', statisticsTitle: 'Кипр в деталях.', statisticsIntro: 'Откуда приезжают люди, кто отдыхает на острове и как он меняется. Начнём с двух вопросов.',
    migration: 'Люди и миграция', immigrationTitle: 'Откуда приезжают люди на Кипр?', immigrationDescription: 'Годовой поток иммиграции по стране предыдущего проживания. Точный источник и доступная разбивка по странам ещё проверяются.', year: 'Год', awaitingMetadata: 'Ожидаем метаданные источника', immigrationUnit: 'Единица: человек · определение проверяется', immigrationSource: 'Источник не подключён. Здесь будут охват, определение и время успешной синхронизации.',
    tourism: 'Путешествия и сезоны', tourismTitle: 'Кто чаще всего отдыхает на Кипре?', tourismDescription: 'Туристические прибытия по стране обычного проживания и сезону. Считаются поездки, а не уникальные посетители.', season: 'Сезон', tourismSource: 'Предполагаемый источник — CYSTAT. Месячные данные, условия использования и глубина истории ещё проверяются.',
    chartPlaceholder: 'Только расположение графика · длина полос не обозначает значения.', loadingTitle: 'Загружаем набор', loadingBody: 'График и фильтры появятся после завершения запроса.', emptyTitle: 'Для выбранного периода нет опубликованных данных', emptyBody: 'Источник и период остаются видимыми. Пропуски не заменяются нулями.', errorTitle: 'Не удалось загрузить набор', errorBody: 'Выбранные фильтры сохраняются. В приложении кнопка повтора снова обратится к нашему backend.', staleBody: 'Здесь останется последний сохранённый набор с датой успешной синхронизации и предупреждением о свежести.',
    aroundYou: 'В городе', parkingTitle: 'Найдите место для парковки.', parkingIntro: 'Карта — для обзора. Список — для подробностей. И всегда время последнего показания.', list: 'Список', map: 'Карта', parkingPlaceholder: 'Название парковки', parkingDetails: 'Здесь будут проверенный адрес и последнее показание.', unknown: 'Нет показания', mapArea: 'Область карты', mapPending: 'Здесь появятся проверенные объекты.', noCoordinates: 'В эскизе нет загрузки карт и вымышленных координат.', parkingSource: 'Источник доступности не подключён. Число свободных мест не отображается.', parkingUnavailable: 'Доступность не подключена. Нет показания — не значит, что мест нет.', parkingStale: 'Старое показание: допустимое значение остаётся с датой. После предельного возраста счётчик скрывается.', parkingLoading: 'Загружаем последнее сохранённое показание…', parkingEmpty: 'Пока нет доступных объектов парковки.', parkingError: 'Не удалось получить показания. При допустимом сохранённом снимке список остаётся доступен.',
    comingSoon: 'Позже', pharmaciesIntro: 'Дежурные аптеки появятся после проверки надёжного официального расписания.', pharmaciesPending: 'Раздел пока недоступен. Ни одна аптека не обозначена как дежурная сейчас.', transparency: 'Откуда берутся цифры', aboutTitle: 'Источники, которые можно проверить.', aboutIntro: 'У каждого набора будут ссылка на оригинал, определение, охват и сведения об обновлении.', publisher: 'Кандидаты в источники', definitions: 'Определения', definitionsValue: 'Иммиграционные потоки и туристические прибытия — разные показатели. Страна проживания не равна гражданству.', license: 'Использование и лицензия', licensePending: 'Проверяются отдельно для каждого ресурса.', lastSync: 'Успешная синхронизация', notSynced: 'Ещё не выполнялась. Это эскиз интерфейса.', scope: 'Географический охват', scopeValue: 'Указываем охват из каждого набора. Не предполагаем, что данные относятся ко всему острову.', footer: 'Открытая информация. Понятные источники.', navigationLabel: 'Основная навигация', languageLabel: 'Язык', parkingViewLabel: 'Вид парковок'
  },
  el: {
    monthlyBreakdown: 'Μηνιαία ανάλυση · μόνο διάταξη',
    skip: 'Μετάβαση στο περιεχόμενο', draft: 'ΠΡΟΣΧΕΔΙΟ · Χωρίς ζωντανά δεδομένα', previewState: 'Κατάσταση', layout: 'Διάταξη', loading: 'Φόρτωση', empty: 'Χωρίς δεδομένα', error: 'Σφάλμα', stale: 'Παρωχημένα δεδομένα',
    statistics: 'Στατιστικά', parking: 'Στάθμευση', pharmacies: 'Φαρμακεία', about: 'Σχετικά και πηγές', publicData: 'Ανοικτά δεδομένα, σαφής εικόνα', statisticsTitle: 'Η Κύπρος από κοντά.', statisticsIntro: 'Από πού έρχονται οι άνθρωποι, ποιοι επισκέπτονται το νησί και πώς αλλάζει. Ξεκινάμε με δύο ερωτήματα.',
    migration: 'Πληθυσμός και μετανάστευση', immigrationTitle: 'Από πού έρχονται όσοι μετακομίζουν στην Κύπρο;', immigrationDescription: 'Ετήσιες μεταναστευτικές ροές ανά χώρα προηγούμενης διαμονής. Η ακριβής πηγή και η διαθέσιμη ανάλυση ανά χώρα βρίσκονται υπό έλεγχο.', year: 'Έτος', awaitingMetadata: 'Αναμονή μεταδεδομένων πηγής', immigrationUnit: 'Μονάδα: άτομα · ο ορισμός ελέγχεται', immigrationSource: 'Η πηγή δεν έχει συνδεθεί. Εδώ θα εμφανίζονται η κάλυψη, ο ορισμός και ο τελευταίος συγχρονισμός.',
    tourism: 'Ταξίδια και εποχές', tourismTitle: 'Ποιοι επισκέπτονται συχνότερα την Κύπρο;', tourismDescription: 'Αφίξεις τουριστών ανά χώρα συνήθους διαμονής και εποχή. Μετρούνται ταξίδια, όχι μοναδικοί επισκέπτες.', season: 'Εποχή', tourismSource: 'Πιθανή πηγή: CYSTAT. Τα μηνιαία δεδομένα, οι όροι χρήσης και η ιστορική κάλυψη ελέγχονται.',
    chartPlaceholder: 'Μόνο διάταξη γραφήματος · οι ράβδοι δεν αντιστοιχούν σε τιμές.', loadingTitle: 'Φόρτωση συνόλου δεδομένων', loadingBody: 'Το γράφημα και τα φίλτρα θα εμφανιστούν μετά την ολοκλήρωση του αιτήματος.', emptyTitle: 'Δεν υπάρχουν δημοσιευμένα δεδομένα για την επιλογή', emptyBody: 'Η πηγή και η περίοδος παραμένουν ορατές. Οι ελλείπουσες τιμές δεν γίνονται μηδενικά.', errorTitle: 'Δεν ήταν δυνατή η φόρτωση', errorBody: 'Τα φίλτρα διατηρούνται. Στην εφαρμογή θα υπάρχει δυνατότητα επανάληψης του αιτήματος προς τον δικό μας διακομιστή.', staleBody: 'Το τελευταίο αποθηκευμένο σύνολο θα παραμένει ορατό με την ημερομηνία συγχρονισμού και προειδοποίηση.',
    aroundYou: 'Στην πόλη', parkingTitle: 'Βρείτε χώρο στάθμευσης.', parkingIntro: 'Χάρτης για μια γενική εικόνα. Λίστα για τις λεπτομέρειες. Πάντα με την ώρα της τελευταίας αναφοράς.', list: 'Λίστα', map: 'Χάρτης', parkingPlaceholder: 'Όνομα χώρου στάθμευσης', parkingDetails: 'Εδώ θα εμφανίζονται η επαληθευμένη διεύθυνση και η τελευταία αναφορά.', unknown: 'Χωρίς αναφορά', mapArea: 'Περιοχή χάρτη', mapPending: 'Εδώ θα εμφανιστούν επαληθευμένες τοποθεσίες.', noCoordinates: 'Το προσχέδιο δεν φορτώνει χάρτες και δεν χρησιμοποιεί επινοημένες συντεταγμένες.', parkingSource: 'Η ζωντανή πηγή δεν έχει συνδεθεί. Δεν εμφανίζονται αριθμοί ελεύθερων θέσεων.', parkingUnavailable: 'Η διαθεσιμότητα δεν έχει συνδεθεί. Η έλλειψη αναφοράς δεν σημαίνει πληρότητα.', parkingStale: 'Παλιά αναφορά: η επιτρεπόμενη τιμή παραμένει με τη χρονική της ένδειξη. Όταν παλιώσει υπερβολικά, αποκρύπτεται.', parkingLoading: 'Φόρτωση της τελευταίας αποθηκευμένης αναφοράς…', parkingEmpty: 'Δεν υπάρχουν διαθέσιμοι χώροι στάθμευσης.', parkingError: 'Δεν ήταν δυνατή η φόρτωση της αναφοράς. Η λίστα παραμένει διαθέσιμη εφόσον υπάρχει έγκυρο αποθηκευμένο στιγμιότυπο.',
    comingSoon: 'Προσεχώς', pharmaciesIntro: 'Τα εφημερεύοντα φαρμακεία θα προστεθούν μετά την επαλήθευση επίσημου προγράμματος.', pharmaciesPending: 'Η λειτουργία δεν είναι ακόμη διαθέσιμη. Κανένα φαρμακείο δεν εμφανίζεται ως εφημερεύον αυτή τη στιγμή.', transparency: 'Η προέλευση των αριθμών', aboutTitle: 'Πηγές που μπορείτε να ελέγξετε.', aboutIntro: 'Κάθε σύνολο θα συνοδεύεται από την αρχική πηγή, τον ορισμό, την κάλυψη και τις πληροφορίες ενημέρωσης.', publisher: 'Πιθανές πηγές', definitions: 'Ορισμοί', definitionsValue: 'Οι μεταναστευτικές ροές και οι αφίξεις τουριστών είναι διαφορετικά μεγέθη. Η χώρα διαμονής δεν είναι η υπηκοότητα.', license: 'Χρήση και άδεια', licensePending: 'Υπό επαλήθευση για κάθε συγκεκριμένο πόρο.', lastSync: 'Τελευταίος συγχρονισμός', notSynced: 'Δεν έχει πραγματοποιηθεί. Πρόκειται για προσχέδιο διεπαφής.', scope: 'Γεωγραφική κάλυψη', scopeValue: 'Εμφανίζουμε την κάλυψη κάθε συνόλου. Δεν θεωρούμε ότι τα δεδομένα αφορούν ολόκληρο το νησί.', footer: 'Ανοικτή πληροφορία. Σαφείς πηγές.', navigationLabel: 'Κύρια πλοήγηση', languageLabel: 'Γλώσσα', parkingViewLabel: 'Προβολή στάθμευσης'
  }
};

const storageKey = 'cyprus-open-prototype-language';
let language = 'en';
try {
  const saved = localStorage.getItem(storageKey);
  if (Object.hasOwn(translations, saved)) language = saved;
} catch {}

const stateSelect = document.getElementById('state');
const navigation = document.getElementById('navigation');
const languages = document.getElementById('languages');
const parkingViews = document.getElementById('parking-view');
const pageIds = ['statistics', 'parking', 'pharmacies', 'about'];

function text(key) {
  return translations[language][key] ?? translations.en[key];
}

function renderDataRegions() {
  for (const region of document.querySelectorAll('.data-region')) {
    region.replaceChildren();
    if (stateSelect.value === 'draft' || stateSelect.value === 'stale') {
      if (stateSelect.value === 'stale') {
        const notice = document.createElement('p');
        notice.className = 'notice';
        notice.textContent = text('staleBody');
        region.append(notice);
      }
      const sketch = document.createElement('div');
      sketch.className = 'chart-sketch';
      const marks = document.createElement('div');
      marks.setAttribute('aria-hidden', 'true');
      marks.style.display = 'grid';
      marks.style.gap = '18px';
      for (const length of [88, 71, 57, 41]) {
        const row = document.createElement('div');
        row.className = 'sketch-row';
        row.innerHTML = '<span class="ghost-label"></span><span class="ghost-track"></span><span class="ghost-value">—</span>';
        row.style.setProperty('--length', `${length}%`);
        marks.append(row);
      }
      const caption = document.createElement('p');
      caption.className = 'chart-caption';
      caption.textContent = text('chartPlaceholder');
      sketch.append(marks, caption);
      region.append(sketch);
      if (region.dataset.kind === 'tourism') {
        const breakdown = document.createElement('div');
        breakdown.className = 'monthly-sketch';
        const heading = document.createElement('p');
        heading.textContent = text('monthlyBreakdown');
        const columns = document.createElement('div');
        columns.className = 'monthly-columns';
        columns.setAttribute('aria-hidden', 'true');
        for (let month = 0; month < 12; month++) {
          const column = document.createElement('span');
          columns.append(column);
        }
        breakdown.append(heading, columns);
        region.append(breakdown);
      }
    } else {
      const message = document.createElement('div');
      message.className = `state-message ${stateSelect.value}`;
      const heading = document.createElement('h3');
      heading.textContent = text(`${stateSelect.value}Title`);
      const description = document.createElement('p');
      description.textContent = text(`${stateSelect.value}Body`);
      message.append(heading, description);
      region.append(message);
    }
  }
  const parkingStatusKeys = { draft: 'parkingUnavailable', loading: 'parkingLoading', empty: 'parkingEmpty', error: 'parkingError', stale: 'parkingStale' };
  document.getElementById('parking-status').textContent = text(parkingStatusKeys[stateSelect.value]);
  document.getElementById('parking-list').hidden = stateSelect.value === 'empty' || selectedParkingView() === 'map';
}

function selectedParkingView() {
  return parkingViews.querySelector('[aria-pressed="true"]').dataset.view;
}

function renderLanguage() {
  document.documentElement.lang = language;
  document.title = `Cyprus Open · ${text('layout')}`;
  document.querySelector('.preview').setAttribute('aria-label', text('previewState'));
  for (const element of document.querySelectorAll('[data-text]')) element.textContent = text(element.dataset.text);
  for (const button of languages.querySelectorAll('button')) button.setAttribute('aria-pressed', String(button.dataset.language === language));
  languages.setAttribute('aria-label', text('languageLabel'));
  navigation.setAttribute('aria-label', text('navigationLabel'));
  parkingViews.setAttribute('aria-label', text('parkingViewLabel'));
  renderDataRegions();
}

function renderPage() {
  const requested = location.hash.slice(1);
  const active = pageIds.includes(requested) ? requested : 'statistics';
  for (const id of pageIds) document.getElementById(id).hidden = id !== active;
  for (const link of navigation.querySelectorAll('a')) {
    if (link.dataset.page === active) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  }
}

languages.addEventListener('click', (event) => {
  const button = event.target.closest('[data-language]');
  if (!button) return;
  language = button.dataset.language;
  try { localStorage.setItem(storageKey, language); } catch {}
  renderLanguage();
});

parkingViews.addEventListener('click', (event) => {
  const button = event.target.closest('[data-view]');
  if (!button) return;
  for (const control of parkingViews.querySelectorAll('button')) control.setAttribute('aria-pressed', String(control === button));
  document.getElementById('parking-map').hidden = button.dataset.view !== 'map';
  renderDataRegions();
});

stateSelect.addEventListener('change', renderDataRegions);
window.addEventListener('hashchange', renderPage);
renderLanguage();
renderPage();

const fs = require("fs");
const path = require("path");

const OUTPUT_PATH = process.argv[2]
  ? path.resolve(process.argv[2])
  : path.join(__dirname, "all-levels-vocabulary.csv");

const SOURCES = {
  cet4: [
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/CET4_1.json",
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/CET4_2.json",
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/CET4_3.json",
  ],
  cet6: [
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/CET6_1.json",
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/CET6_2.json",
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/CET6_3.json",
  ],
  toefl: [
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/TOEFL_2.json",
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/TOEFL_3.json",
  ],
  ielts: [
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/IELTS_2.json",
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/IELTS_3.json",
  ],
  kaoyan: [
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/KaoYan_1.json",
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/KaoYan_2.json",
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/KaoYan_3.json",
  ],
  gre: [
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/GRE_2.json",
    "https://raw.githubusercontent.com/KyleBing/english-vocabulary/master/json_original/json-full/GRE_3.json",
  ],
};

function normalizeText(value) {
  if (value === null || value === undefined) {
    return "";
  }

  return String(value).replace(/\r/g, " ").replace(/\n/g, " ").replace(/\s+/g, " ").trim();
}

function buildMeaning(transList) {
  if (!Array.isArray(transList)) {
    return "";
  }

  const parts = [];
  for (const item of transList) {
    const tran = normalizeText(item?.tranCn);
    const pos = normalizeText(item?.pos);
    if (!tran) {
      continue;
    }
    parts.push(pos ? `${pos}. ${tran}` : tran);
  }

  return Array.from(new Set(parts)).join("; ");
}

function buildPhonetic(content) {
  const candidates = [content?.phone, content?.ukphone, content?.usphone]
    .map(normalizeText)
    .filter(Boolean);

  if (candidates.length === 0) {
    return "";
  }

  return `/${Array.from(new Set(candidates)).join(" | ")}/`;
}

function buildExample(content) {
  const sentence = content?.sentence?.sentences?.[0]?.sContent;
  if (sentence) {
    return normalizeText(sentence);
  }

  const realExam = content?.realExamSentence?.sentences?.[0]?.sContent;
  if (realExam) {
    return normalizeText(realExam);
  }

  return "";
}

function upsertRow(store, moduleCode, word, phonetic, meaning, example) {
  const cleanWord = normalizeText(word);
  const cleanMeaning = normalizeText(meaning);
  const cleanPhonetic = normalizeText(phonetic);
  const cleanExample = normalizeText(example);

  if (!cleanWord || !cleanMeaning) {
    return;
  }

  const key = `${moduleCode}|${cleanWord.toLowerCase()}`;
  const existing = store.get(key);
  if (!existing) {
    store.set(key, {
      module_code: moduleCode,
      word: cleanWord,
      phonetic: cleanPhonetic,
      meaning: cleanMeaning,
      example: cleanExample,
    });
    return;
  }

  if (!existing.phonetic && cleanPhonetic) {
    existing.phonetic = cleanPhonetic;
  }
  if (!existing.example && cleanExample) {
    existing.example = cleanExample;
  }
  if (!existing.meaning && cleanMeaning) {
    existing.meaning = cleanMeaning;
  }
}

async function fetchJson(url) {
  const response = await fetch(url, {
    headers: {
      "User-Agent": "Codex",
      Accept: "application/json,text/plain;q=0.9,*/*;q=0.8",
    },
  });

  if (!response.ok) {
    throw new Error(`Request failed ${response.status} for ${url}`);
  }

  return response.json();
}

async function addKyleBingModule(rowsByKey, moduleCode, urls) {
  for (const url of urls) {
    console.log(`Source: ${url}`);
    const items = await fetchJson(url);
    for (const entry of items) {
      const word =
        entry?.headWord ||
        entry?.content?.word?.wordHead ||
        "";
      const wordContent = entry?.content?.word?.content;
      if (!wordContent) {
        continue;
      }

      upsertRow(
        rowsByKey,
        moduleCode,
        word,
        buildPhonetic(wordContent),
        buildMeaning(wordContent.trans),
        buildExample(wordContent)
      );
    }
  }
}

function escapeCsv(value) {
  const text = value === null || value === undefined ? "" : String(value);
  return `"${text.replace(/"/g, '""')}"`;
}

async function main() {
  const rowsByKey = new Map();

  for (const [moduleCode, urls] of Object.entries(SOURCES)) {
    console.log(`Fetching ${moduleCode}...`);
    await addKyleBingModule(rowsByKey, moduleCode, urls);
  }

  const rows = Array.from(rowsByKey.values()).sort((a, b) => {
    if (a.module_code !== b.module_code) {
      return a.module_code.localeCompare(b.module_code);
    }
    return a.word.localeCompare(b.word);
  });

  if (rows.length === 0) {
    throw new Error("No vocabulary rows were generated.");
  }

  fs.mkdirSync(path.dirname(OUTPUT_PATH), { recursive: true });
  const header = ["module_code", "word", "phonetic", "meaning", "example"];
  const csvLines = [header.join(",")];
  for (const row of rows) {
    csvLines.push(
      [
        escapeCsv(row.module_code),
        escapeCsv(row.word),
        escapeCsv(row.phonetic),
        escapeCsv(row.meaning),
        escapeCsv(row.example),
      ].join(",")
    );
  }

  fs.writeFileSync(OUTPUT_PATH, csvLines.join("\r\n"), "utf8");
  console.log(`CSV generated: ${OUTPUT_PATH}`);

  const summary = rows.reduce((acc, row) => {
    acc[row.module_code] = (acc[row.module_code] || 0) + 1;
    return acc;
  }, {});

  console.log("Row summary:");
  for (const moduleCode of Object.keys(summary).sort()) {
    console.log(`${moduleCode}: ${summary[moduleCode]}`);
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : String(error));
  process.exit(1);
});

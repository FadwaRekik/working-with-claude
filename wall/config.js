// Build Wall configuration. Pick ONE backend.
// A. Supabase (see supabase/schema.sql): project URL + publishable (or legacy anon) key. Safe to publish:
//    row-level security limits it to adding ideas and reading the wall.
// B. Google Sheet via Apps Script (see apps-script/Code.gs): set appsScriptUrl to the Web app URL ending in /exec.
window.BUILD_WALL_CONFIG = {
  appsScriptUrl: "",
  supabaseUrl: "https://ahaiqfwldvayumwtjjvo.supabase.co",
  supabaseAnonKey: "sb_publishable_7vTaRiko33iytQRGC22pPw_YTyzYshp",
};

import { useOrganizerContext } from "../context";

export function OnboardingPanel() {
  const { orgName, setOrgName, busy, createOrganizer } = useOrganizerContext();
  return (
    <section className="panel onboarding">
      <span className="eyebrow">FIRST STEP</span>
      <h2>创建你的主办方</h2>
      <p>每个负责人账号仅可管理一个主办方。</p>
      <form onSubmit={createOrganizer}>
        <input
          value={orgName}
          onChange={(e) => setOrgName(e.target.value)}
          placeholder="主办方名称"
          required
        />
        <button className="primary" disabled={busy}>
          创建主办方
        </button>
      </form>
    </section>
  );
}

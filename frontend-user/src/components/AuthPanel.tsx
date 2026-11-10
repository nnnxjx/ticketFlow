import { useUserContext } from "../context";

export function AuthPanel() {
  const {
    phone,
    setPhone,
    password,
    setPassword,
    registering,
    setRegistering,
    busy,
    auth,
  } = useUserContext();
  return (
    <section className="auth">
      <div>
        <span className="eyebrow">MEMBER ACCESS</span>
        <h2>先登录，再赴约。</h2>
        <p>锁定座位、模拟支付，并查看属于你的电子票。</p>
      </div>
      <form onSubmit={auth}>
        <label>
          手机号
          <input
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            pattern="1[3-9][0-9]{9}"
            required
            placeholder="11 位手机号"
          />
        </label>
        <label>
          密码
          <input
            type="password"
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            placeholder="至少 8 位"
          />
        </label>
        <button className="primary" disabled={busy}>
          {registering ? "注册" : "登录"}
        </button>
        <button
          type="button"
          className="link"
          onClick={() => setRegistering(!registering)}
        >
          {registering ? "已有账号？登录" : "创建账号"}
        </button>
      </form>
    </section>
  );
}

import { useOrganizerContext } from "../context";
import { Toast } from "./Toast";

export function LoginPage() {
  const {
    phone,
    setPhone,
    password,
    setPassword,
    register,
    setRegister,
    busy,
    notice,
    setNotice,
    auth,
  } = useOrganizerContext();
  return (
    <main className="login">
      <Toast message={notice} dismiss={() => setNotice("")} />
      <div className="login-brand">
        T{" "}
        <span>
          TicketFlow
          <br />
          <small>ORGANIZER STUDIO</small>
        </span>
      </div>
      <div className="login-card">
        <span className="eyebrow">WELCOME BACK</span>
        <h1>
          为每一场现场
          <br />
          做好准备。
        </h1>
        <p>登录主办方工作台，管理演出、座位与现场核验。</p>
        <form onSubmit={auth}>
          <label>
            手机号
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              pattern="1[3-9][0-9]{9}"
              required
            />
          </label>
          <label>
            密码
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              required
            />
          </label>
          <button className="primary" disabled={busy}>
            {register ? "注册账号" : "登录工作台"}
          </button>
        </form>
        <button className="link" onClick={() => setRegister(!register)}>
          {register ? "已有账号？登录" : "创建负责人账号"}
        </button>
      </div>
    </main>
  );
}

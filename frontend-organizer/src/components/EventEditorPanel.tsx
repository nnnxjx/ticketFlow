import { useOrganizerContext } from "../context";
import { status } from "../model";

export function EventEditorPanel() {
  const {
    shows,
    show,
    setShow,
    title,
    setTitle,
    category,
    setCategory,
    description,
    setDescription,
    setSessions,
    setSession,
    busy,
    selectShow,
    eventSubmit,
    eventAction,
  } = useOrganizerContext();
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>演出列表</h2>
        <button
          onClick={() => {
            setShow(null);
            setTitle("");
            setCategory("");
            setDescription("");
            setSessions([]);
            setSession(null);
          }}
        >
          ＋ 新建
        </button>
      </div>
      {shows.map((x) => (
        <button
          key={x.id}
          className={`list-row ${show?.id === x.id ? "active" : ""}`}
          onClick={() => void selectShow(x)}
        >
          <strong>{x.title}</strong>
          <small>
            {status[x.status] || x.status} · {x.category || "未分类"}
          </small>
        </button>
      ))}
      <form className="form" onSubmit={eventSubmit}>
        <h3>{show ? "编辑演出" : "新建演出"}</h3>
        <label>
          名称
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </label>
        <label>
          分类
          <input
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          />
        </label>
        <label>
          介绍
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
          />
        </label>
        <button className="primary" disabled={busy}>
          保存演出
        </button>
      </form>
      {show && (
        <div className="button-row">
          <button onClick={() => void eventAction("publish")} disabled={busy}>
            发布
          </button>
          <button onClick={() => void eventAction("off-shelf")} disabled={busy}>
            下架
          </button>
          <button onClick={() => void eventAction("delete")} disabled={busy}>
            删除草稿
          </button>
        </div>
      )}
    </section>
  );
}

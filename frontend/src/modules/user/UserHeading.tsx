import type { ReactNode } from 'react';

/** 用户模块独有的标题排版，装饰与可读文字分开。 */
export function UserHeading({ section, title, description }: { section: string; title: ReactNode; description: string }) {
    return <div className="page-intro user-intro">
        <span className="eyebrow user-eyebrow"><span className="user-loop-mark" aria-hidden="true">↻</span>CAMPUS LOOP<span className="user-eyebrow-divider" aria-hidden="true"/>{section}</span>
        <h1>{title}<span className="user-title-stop" aria-hidden="true">。</span></h1>
        <p>{description}</p>
    </div>;
}

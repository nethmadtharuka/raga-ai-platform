import type { CSSProperties, ReactNode } from 'react'
import { Mascot, Ribbon, Scribble, SpinBadge } from '../components/Art'
import { m } from 'motion/react'
import { SoundToggle, ThemeToggle } from '../components/Chrome'
import { useToast } from '../components/Toast'
import { EASE, Reveal, RevealGroup, RevealItem, revealItem } from '../lib/motion'
import { Icon, type IconName } from '../components/Icon'

const FEATS: [string, string, string][] = [
  ['v', 'Ask in plain words', 'Type a question the way you would ask a colleague. No search syntax, no folders to dig through.'],
  ['l', 'Every answer shows its source', 'Numbered references point to the exact passage, so you can check before you trust.'],
  ['p', 'Summaries in one click', 'Get a short summary of the sources you pick, written from your own files.'],
  ['v', 'You choose what counts', 'Switch documents on or off. RAGMe! tells you when an answer leans on a document you left out.'],
  ['l', 'Works with your files', 'Text, Markdown, CSV, HTML and JSON files, or paste text straight in. PDF and Word support is on the way.'],
  ['p', 'Keep what matters', 'Save good answers as notes and come back to them later.'],
];
const USES: [IconName, string, string, string][] = [
  ['cap', 'Students', 'Turn lecture notes into answers you can check.', '“What will the exam cover?”'],
  ['users', 'Teams', 'Find policy answers without waiting on someone else.', '“How much leave do new staff get?”'],
  ['flask', 'Researchers', 'Compare reports, notes and survey results quickly.', '“Where do these two studies disagree?”'],
  ['brief', 'Business owners', 'Make sense of feedback, exports and meeting notes.', '“What did customers ask for most?”'],
]
const FAQS: [string, string][] = [
  ['What can I upload?', 'Text, Markdown, CSV, HTML and JSON files up to 1 MB each, or text you paste in. Support for PDF, Word and Excel files and for web links is coming soon.'],
  ['Will RAGMe! make things up?', 'RAGMe! answers from the documents you add and shows where each answer came from. If your documents do not contain the answer, it tells you so instead of guessing.'],
  ['Can I choose which documents it uses?', 'Yes. Tick or untick any source in your notebook. If an answer relies on a document you have switched off, RAGMe! tells you.'],
  ['Do I need technical skills?', 'No. If you can send a message, you can use RAGMe!.'],
  ['Can I use it on my phone?', 'Yes. The notebook switches to a simple three-tab layout on small screens.'],
  ['Where are my documents kept?', 'The text of your documents is stored on the RAGMe! server so it can answer your questions. Your notebooks, notes and source lists are saved in this browser.'],
]

function ScrollBtn({ to, className, style, children }: { to: string; className?: string; style?: CSSProperties; children: ReactNode }) {
  const go = () =>
    document.getElementById(to)?.scrollIntoView({ behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' })
  return (
    <button type="button" className={className} style={style} onClick={go}>
      {children}
    </button>
  )
}

const Arr = () => (
  <span className="arr">
    <Icon name="arrow" size={14} />
  </span>
)

export function Landing() {
  const toast = useToast()
  const soon = () => toast('This page is coming soon.')
  return (
    <>
      <header className="hero" id="top">
        <div className="wrap">
          <m.nav className="lnav" aria-label="Main" initial={{ opacity: 0, y: -14 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, ease: EASE }}>
            <a className="logo" href="#home" aria-label="RAGMe! home">
              RAGMe!
            </a>
            <div className="links">
              <ScrollBtn to="product">Product</ScrollBtn>
              <ScrollBtn to="how">How it works</ScrollBtn>
              <ScrollBtn to="uses">Who it’s for</ScrollBtn>
              <ScrollBtn to="faq">FAQ</ScrollBtn>
            </div>
            <div className="row g8">
              <SoundToggle inverse />
              <ThemeToggle inverse />
              <a className="btn sm lime" href="#notebooks">
                Open RAGMe!
              </a>
            </div>
          </m.nav>
        </div>
        <div className="hero-word">
          <span className="scrib-l">
            <Scribble color="var(--b-navy)" width={18} draw />
          </span>
          <span className="scrib-r">
            <Scribble color="var(--b-lime)" width={14} d="M10 120C50 30 80 150 120 60S190 20 250 90" draw />
          </span>
          <h1 aria-label="RAGMe!">
            {[...'RAGMe!'].map((c, i) => (
              <m.span
                key={i}
                className="hl"
                aria-hidden="true"
                initial={{ y: '45%', opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                transition={{ delay: 0.1 + i * 0.06, duration: 0.8, ease: EASE }}
              >
                {c}
              </m.span>
            ))}
          </h1>
          <m.div
            className="badge-pos"
            initial={{ scale: 0, rotate: -120 }}
            animate={{ scale: 1, rotate: 0 }}
            transition={{ delay: 0.7, type: 'spring', stiffness: 160, damping: 14 }}
          >
            <SpinBadge />
          </m.div>
        </div>
        <div className="wrap">
          <m.p className="hero-sub" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5, duration: 0.6, ease: EASE }}>
            Upload your documents, ask anything, and get clear answers that show <em>exactly</em> where they came from.
          </m.p>
          <m.div className="row center g12 wrapx mt24" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.62, duration: 0.6, ease: EASE }}>
            <a className="btn lime" href="#notebooks">
              Try RAGMe! <Arr />
            </a>
            <ScrollBtn to="how" className="btn ghost light">
              See how it works
            </ScrollBtn>
          </m.div>
        </div>
      </header>

      <main>
        <section className="band dark nopad">
          <Ribbon />
        </section>
        <section className="band dark" aria-label="Highlights">
          <div className="wrap">
            <RevealGroup className="tiles">
              <Tile c="v" icon="file" title="Your documents" text="Notes, exports, web pages and data files." />
              <Tile c="p" icon="chat" title="Plain questions" text="Ask the way you talk." />
              <Tile c="l" icon="check" title="Cited answers" text="See the passage behind every answer." />
              <Tile c="w" icon="shield" title="Honest replies" text="If it’s not in your files, RAGMe! says so." />
            </RevealGroup>
          </div>
        </section>

        <section className="band dark" id="product">
          <div className="wrap two">
            <Reveal>
              <h2 className="big">
                Your files, <em>answering</em> back
              </h2>
              <p className="lead">
                Add the documents you already have. RAGMe! reads them and answers from exactly that material, so you can ask questions instead of searching page by page.
              </p>
              <div className="row g12 wrapx mt24">
                <a className="btn lime" href="#notebooks">
                  Start a notebook <Arr />
                </a>
              </div>
            </Reveal>
            <m.figure
              className="mock"
              aria-label="Example of a RAGMe! notebook"
              initial={{ opacity: 0, x: 40, rotate: 2 }}
              whileInView={{ opacity: 1, x: 0, rotate: 0 }}
              viewport={{ once: true, amount: 0.3 }}
              transition={{ duration: 0.8, ease: EASE }}
            >
              <div className="ms">
                <h3>Sources</h3>
                <ul className="col g8">
                  <li>
                    <i />
                    Employee Handbook
                  </li>
                  <li>
                    <i />
                    Leave Policy.md
                  </li>
                  <li>
                    <i />
                    Remote Work.html
                  </li>
                </ul>
              </div>
              <RevealGroup className="mc">
                <m.div className="pv-q" variants={revealItem}>How many days of annual leave do new employees get?</m.div>
                <m.div className="pv-a" variants={revealItem}>
                  <Mascot mood="happy" color="violet" size={34} />
                  <p>
                    In the first year, leave is granted in proportion to the months you have completed<span className="mk">1</span>. After that you get 14 paid days a year
                    <span className="mk">2</span>.
                  </p>
                </m.div>
                <m.div className="pv-s" variants={revealItem}>
                  <span>
                    <b>1</b> Leave Policy.md
                  </span>
                  <span>
                    <b>2</b> Leave Policy.md
                  </span>
                </m.div>
              </RevealGroup>
            </m.figure>
          </div>
        </section>

        <section className="band light" id="features">
          <div className="wrap">
            <Reveal className="sh">
              <h2 className="big dk">
                Made to be <em>understood</em>
              </h2>
              <p>Everything you need to get answers from your own material, and nothing you have to learn first.</p>
            </Reveal>
<RevealGroup className="bento">
  {FEATS.map(([c, h, p]) => (
    <RevealItem key={h}>
      <article className={`bt ${c}`}>
        <h3>{h}</h3>
        <p>{p}</p>
      </article>
    </RevealItem>
  ))}
</RevealGroup>
</div>
</section>

        <section className="band dark" id="how">
          <div className="wrap">
            <Reveal className="sh">
              <h2 className="big">
                How it <em>works</em>
              </h2>
              <p>Three steps. No setup, no training.</p>
            </Reveal>
            <RevealGroup className="tilts">
              <Tilt c="l" r="-2.5deg" n={1} title="Add your documents" text="Drop in files or paste text. They stay together in one notebook." />
              <Tilt c="v" r="1.8deg" n={2} title="Ask a question" text="Type anything, or tap a suggested question to get started." />
              <Tilt c="p" r="-1.5deg" n={3} title="Check the sources" text="Open any numbered reference to see the exact passage that supports the answer." />
            </RevealGroup>
          </div>
        </section>

        <section className="band navy" id="uses">
          <div className="wrap">
            <Reveal className="sh">
              <h2 className="big">
                Who it’s <em>for</em>
              </h2>
              <p>Anyone who has more reading than time.</p>
            </Reveal>
            <RevealGroup className="uses">
              {USES.map(([icon, h, p, q]) => (
                <RevealItem key={h}>
                <article className="use">
                  <span className="tic">
                    <Icon name={icon} size={22} />
                  </span>
                  <h3>{h}</h3>
                  <p>{p}</p>
                  <blockquote>{q}</blockquote>
                </article>
                </RevealItem>
              ))}
            </RevealGroup>
          </div>
        </section>

        <section className="band lime">
          <div className="wrap trust">
            <Reveal>
              <h2 className="big ink">
                No guessing. <em>No made-up facts.</em>
              </h2>
              <p className="lead">RAGMe! only answers from what you give it. When your documents don’t cover a question, it says so and points you to what it did find.</p>
            </Reveal>
            <m.figure
              className="bub-demo"
              aria-label="Example of an honest reply"
              initial={{ opacity: 0, scale: 0.9, rotate: -2 }}
              whileInView={{ opacity: 1, scale: 1, rotate: 0 }}
              viewport={{ once: true, amount: 0.4 }}
              transition={{ type: 'spring', stiffness: 140, damping: 16 }}
            >
              <div className="row g12">
                <Mascot mood="shrug" color="violet" size={64} />
                <b style={{ fontSize: 19 }}>I couldn’t find that in your sources.</b>
              </div>
              <p>The selected documents don’t mention parental leave. One file could not be read: Benefits Overview.pdf.</p>
              <a className="btn dark sm" href="#notebooks" style={{ alignSelf: 'flex-start' }}>
                Try it yourself
              </a>
            </m.figure>
          </div>
        </section>

        <section className="band light" id="faq">
          <div className="wrap">
            <Reveal className="sh">
              <h2 className="big dk">
                Good <em>questions</em>
              </h2>
            </Reveal>
            <RevealGroup className="faq">
              {FAQS.map(([q, a], i) => (
                <m.details key={q} open={i === 0} variants={revealItem}>
                  <summary>{q}</summary>
                  <p>{a}</p>
                </m.details>
              ))}
            </RevealGroup>
          </div>
        </section>

        <section className="band lime cta">
          <div className="wrap">
            <Reveal>
            <h2 className="mega">
              Ask your <em>documents.</em>
            </h2>
            <div className="row center g12 wrapx mt24">
              <a className="btn dark" href="#notebooks">
                Open RAGMe! <Arr />
              </a>
              <button type="button" className="btn ghost ink" onClick={() => toast('Accounts are coming soon. Your notebooks are saved in this browser for now.')}>
                Create an account
              </button>
            </div>
            </Reveal>
          </div>
        </section>
      </main>

      <footer className="foot">
        <div className="wrap">
          <Reveal className="foot-word">
            <span aria-hidden="true">RAGMe!</span>
          </Reveal>
          <div className="foot-row">
            <span>© 2026 RAGMe!. Answers from your own documents.</span>
            <div className="row g12 wrapx">
              <ScrollBtn to="product">Product</ScrollBtn>
              <ScrollBtn to="faq">FAQ</ScrollBtn>
              <button type="button" onClick={soon}>
                Privacy
              </button>
              <button type="button" onClick={soon}>
                Terms
              </button>
              <button type="button" onClick={soon}>
                Contact
              </button>
            </div>
          </div>
        </div>
      </footer>
    </>
  )
}

function Tile({ c, icon, title, text }: { c: string; icon: IconName; title: string; text: string }) {
  return (
    <RevealItem>
    <div className={`tile ${c}`}>
      <span className="tic">
        <Icon name={icon} size={22} />
      </span>
      <div>
        <h3>{title}</h3>
        <p>{text}</p>
      </div>
    </div>
    </RevealItem>
  )
}

function Tilt({ c, r, n, title, text }: { c: string; r: string; n: number; title: string; text: string }) {
  return (
    <RevealItem>
    <article className={`tilt ${c}`} style={{ '--r': r } as CSSProperties}>
      <span className="tn">{n}</span>
      <h3>{title}</h3>
      <p>{text}</p>
    </article>
    </RevealItem>
  )
}

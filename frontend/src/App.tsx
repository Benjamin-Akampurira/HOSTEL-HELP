import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import {
  ArrowDownUp, Bell, BookOpen, Check, CheckCheck, ChevronRight, CircleAlert,
  Clock3, FileWarning, HandHeart, Home, LogOut, Menu, Plus, RefreshCw,
  Search, ShieldCheck, Star, Users, X,
} from 'lucide-react'
import './App.css'

type Student = { id: number; fullName: string; email: string; registrationNumber: string; hostelName: string; hostelBlock: string; roomNumber: string; trustScore: number }
type Auth = { id: number; fullName: string; email: string; role: 'STUDENT' | 'WARDEN'; student: Student | null; wardenPhone: string | null }
type Member = { id: number; fullName: string; hostelName: string; hostelBlock: string; roomNumber: string }
type RequestItem = { id: number; requesterId: number; requesterName: string; itemName: string; category: string; description: string; urgency: 'NOW' | 'TODAY' | 'THIS_WEEK'; status: string; hostelBlock: string; expectedReturnDate: string; createdAt: string; offerCount: number }
type Offer = { id: number; requestId: number; requesterId: number; itemName: string; helperId: number; helperName: string; message: string | null; status: string; offeredAt: string }
type Borrowing = { id: number; requestId: number; itemName: string; borrowerId: number; borrowerName: string; helperId: number; helperName: string; expectedReturnDate: string; receivedAt: string | null; returnInitiatedAt: string | null; returnedAt: string | null; status: string; rated: boolean }
type Notification = { id: number; message: string; type: string; read: boolean; createdAt: string }
type Report = { id: number; reporterId: number; reporterName: string; borrowingTransactionId: number; itemName: string; reason: string; description: string; status: string; createdAt: string; resolvedAt: string | null; resolutionNotes: string | null }
type Rating = { id: number; borrowerId: number; borrowerName: string; helperId: number; helperName: string; borrowingTransactionId: number; score: number; comment: string | null; createdAt: string }
type WardenMember = Member & { email: string; registrationNumber: string; trustScore: number; createdAt: string; enabled: boolean }
type WardenReportDetails = { report: Report; reporter: WardenMember; borrower: WardenMember; helper: WardenMember; request: { id: number; itemName: string; category: string; description: string; urgency: string; status: string; hostelBlock: string; expectedReturnDate: string | null; createdAt: string }; offer: { id: number; message: string | null; status: string; offeredAt: string; acceptedAt: string | null }; borrowing: Borrowing }
type WardenOverview = { memberCount: number; requestCount: number; offerCount: number; borrowingCount: number; ratingCount: number; notificationCount: number; reportCount: number; recentRequests: RequestItem[]; recentOffers: Offer[]; recentBorrowings: Borrowing[]; recentRatings: Rating[]; members: WardenMember[]; reports: Report[] }
type Page = 'home' | 'requests' | 'members' | 'offers' | 'borrowings' | 'notifications' | 'profile' | 'reports'

let csrfToken = ''
const today = new Date().toISOString().slice(0, 10)

async function getCsrfToken() {
  if (csrfToken) return csrfToken
  const response = await fetch('/api/auth/csrf', { credentials: 'include' })
  if (!response.ok) throw new Error('The backend is not responding. Start the Java API and try again.')
  const data = await response.json() as { message: string }
  csrfToken = data.message
  return csrfToken
}

async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  if (options.body) headers.set('Content-Type', 'application/json')
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers.set('X-CSRF-TOKEN', await getCsrfToken())
  const response = await fetch(path, { ...options, method, headers, credentials: 'include' })
  if (response.status === 204) return undefined as T
  const body = await response.json().catch(() => null) as { error?: string; message?: string } | null
  if (!response.ok) throw new Error(body?.error ?? body?.message ?? `Request failed (${response.status}).`)
  return body as T
}

function App() {
  const [auth, setAuth] = useState<Auth | null>(null)
  const [page, setPage] = useState<Page>('home')
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')
  const [requests, setRequests] = useState<RequestItem[]>([])
  const [mine, setMine] = useState<RequestItem[]>([])
  const [offers, setOffers] = useState<Offer[]>([])
  const [borrowings, setBorrowings] = useState<Borrowing[]>([])
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [reports, setReports] = useState<Report[]>([])
  const [members, setMembers] = useState<Member[]>([])
  const [wardenOverview, setWardenOverview] = useState<WardenOverview | null>(null)
  const [reportDetails, setReportDetails] = useState<Record<number, WardenReportDetails>>({})
  const [expandedReportId, setExpandedReportId] = useState<number | null>(null)
  const [loadingReportId, setLoadingReportId] = useState<number | null>(null)
  const [search, setSearch] = useState('')
  const [memberSearch, setMemberSearch] = useState('')
  const [urgency, setUrgency] = useState('')
  const [modal, setModal] = useState<'request' | 'rating' | 'report' | null>(null)
  const [selectedBorrowing, setSelectedBorrowing] = useState<Borrowing | null>(null)
  const [mobileNav, setMobileNav] = useState(false)

  useEffect(() => {
    let active = true
    getCsrfToken().then(() => api<Auth>('/api/auth/me'))
      .then((session) => { if (active) { setAuth(session); if (session.role === 'WARDEN') setPage('reports') } })
      .catch((reason: unknown) => {
        if (!active) return
        setAuth(null)
        if (reason instanceof Error && reason.message.startsWith('The backend is not responding')) {
          setError(reason.message)
        }
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!auth) return
    let active = true
    const query = new URLSearchParams()
    if (search.trim()) query.set('search', search.trim())
    if (urgency) query.set('urgency', urgency)
    const suffix = query.toString() ? `?${query}` : ''
    const tasks = auth.role === 'WARDEN'
      ? [api<WardenOverview>('/api/warden/overview').then((value) => { if (active) { setWardenOverview(value); setReports(value.reports) } })]
      : [
          api<RequestItem[]>(`/api/requests${suffix}`).then((value) => { if (active) setRequests(value) }),
          api<RequestItem[]>('/api/requests/mine').then((value) => { if (active) setMine(value) }),
          api<Offer[]>('/api/offers').then((value) => { if (active) setOffers(value) }),
          api<Borrowing[]>('/api/borrowings').then((value) => { if (active) setBorrowings(value) }),
          api<Notification[]>('/api/notifications').then((value) => { if (active) setNotifications(value) }),
          api<Member[]>('/api/students').then((value) => { if (active) setMembers(value) }),
        ]
    Promise.all(tasks).catch((reason: unknown) => {
      if (active) setError(reason instanceof Error ? reason.message : 'Could not load your data.')
    })
    return () => { active = false }
  }, [auth, search, urgency])

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(''), 3200)
    return () => window.clearTimeout(timer)
  }, [toast])

  async function refresh() {
    if (!auth) return
    setBusy(true); setError('')
    try {
      if (auth.role === 'WARDEN') {
        const overview = await api<WardenOverview>('/api/warden/overview')
        setWardenOverview(overview); setReports(overview.reports)
      }
      else {
        const [requestData, mineData, offerData, borrowingData, notificationData, memberData] = await Promise.all([
          api<RequestItem[]>(`/api/requests${search ? `?search=${encodeURIComponent(search)}` : ''}`),
          api<RequestItem[]>('/api/requests/mine'), api<Offer[]>('/api/offers'),
          api<Borrowing[]>('/api/borrowings'), api<Notification[]>('/api/notifications'),
          api<Member[]>('/api/students'),
        ])
        setRequests(requestData); setMine(mineData); setOffers(offerData)
        setBorrowings(borrowingData); setNotifications(notificationData)
        setMembers(memberData)
      }
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Could not refresh data.') }
    finally { setBusy(false) }
  }

  async function submitAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError('')
    const form = new FormData(event.currentTarget)
    try {
      if (authMode === 'register') await api('/api/auth/register', { method: 'POST', body: JSON.stringify({
        fullName: form.get('fullName'), registrationNumber: form.get('registrationNumber'), email: form.get('email'),
        password: form.get('password'), confirmPassword: form.get('confirmPassword'), hostelName: form.get('hostelName'),
        hostelBlock: form.get('hostelBlock'), roomNumber: form.get('roomNumber'),
      }) })
      const session = await api<Auth>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email: form.get('email'), password: form.get('password') }) })
      setAuth(session); if (session.role === 'WARDEN') setPage('reports'); setToast(`Welcome, ${session.fullName.split(' ')[0]}.`)
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Sign in failed.') }
    finally { setBusy(false) }
  }

  async function runAction(action: () => Promise<unknown>, message: string) {
    setBusy(true); setError('')
    try { await action(); await refresh(); setToast(message); setModal(null) }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'That action could not be completed.') }
    finally { setBusy(false) }
  }

  async function toggleReportDetails(reportId: number) {
    if (expandedReportId === reportId) {
      setExpandedReportId(null)
      return
    }
    setExpandedReportId(reportId)
    if (reportDetails[reportId]) return
    setLoadingReportId(reportId)
    try {
      const details = await api<WardenReportDetails>(`/api/warden/reports/${reportId}`)
      setReportDetails((current) => ({ ...current, [reportId]: details }))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not load report details.')
      setExpandedReportId(null)
    } finally {
      setLoadingReportId(null)
    }
  }

  async function createRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    await runAction(() => api('/api/requests', { method: 'POST', body: JSON.stringify({
      itemName: form.get('itemName'), category: form.get('category'), description: form.get('description'),
      urgency: form.get('urgency'), expectedReturnDate: form.get('expectedReturnDate'),
    }) }), 'Request shared with all hostel blocks.')
  }

  async function submitRating(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedBorrowing) return
    const form = new FormData(event.currentTarget)
    await runAction(() => api(`/api/borrowings/${selectedBorrowing.id}/rating`, { method: 'POST', body: JSON.stringify({
      score: Number(form.get('score')), comment: form.get('comment'),
    }) }), 'Rating submitted. Thank you for closing the loop.')
  }

  async function submitReport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedBorrowing) return
    const form = new FormData(event.currentTarget)
    await runAction(() => api('/api/reports', { method: 'POST', body: JSON.stringify({
      borrowingTransactionId: selectedBorrowing.id, reason: form.get('reason'), description: form.get('description'),
    }) }), 'Your report was sent to the warden.')
  }

  async function logout() {
    try { await api('/api/auth/logout', { method: 'POST' }) } catch { /* clear local view even if the server session expired */ }
    csrfToken = ''; setAuth(null); setPage('home'); setError(''); setToast('You are signed out.')
  }

  if (loading) return <div className="boot-screen"><div className="brand-mark"><HandHeart size={22} /></div><p>Connecting to Hostel Help</p></div>
  if (!auth) return <AuthScreen mode={authMode} setMode={setAuthMode} onSubmit={submitAuth} busy={busy} error={error} />

  const isWarden = auth.role === 'WARDEN'
  const nav: { id: Page; label: string; icon: typeof Home; count?: number }[] = isWarden
    ? [{ id: 'reports', label: 'Reports', icon: FileWarning, count: reports.filter((report) => report.status === 'OPEN').length }]
    : [
        { id: 'home', label: 'Overview', icon: Home },
        { id: 'requests', label: 'Browse requests', icon: Search },
        { id: 'members', label: 'Member directory', icon: Users },
        { id: 'offers', label: 'Offers', icon: HandHeart, count: offers.filter((offer) => offer.requesterId === auth.id && offer.status === 'PENDING').length },
        { id: 'borrowings', label: 'Borrowing', icon: ArrowDownUp, count: borrowings.filter((item) => item.status !== 'RETURNED').length },
        { id: 'notifications', label: 'Notifications', icon: Bell, count: notifications.filter((item) => !item.read).length },
        { id: 'profile', label: 'My profile', icon: BookOpen },
      ]

  return <div className="app-shell">
    <aside className={`sidebar ${mobileNav ? 'sidebar-open' : ''}`}>
      <a className="brand" href="#home" onClick={(event) => { event.preventDefault(); setPage(isWarden ? 'reports' : 'home') }}><span className="brand-mark"><HandHeart size={22} strokeWidth={2.2} /></span><span>hostel<span className="brand-light">help</span><small>NEIGHBOURS, NOT STRANGERS</small></span></a>
      <div className="side-caption">YOUR SPACE</div>
      <nav className="side-nav" aria-label="Main navigation">{nav.map(({ id, label, icon: Icon, count }) => <button key={id} className={`nav-link ${page === id ? 'nav-active' : ''}`} onClick={() => { setPage(id); setMobileNav(false) }}><Icon size={18} /><span>{label}</span>{count ? <b>{count}</b> : null}</button>)}</nav>
      <div className="sidebar-bottom"><div className="block-note"><ShieldCheck size={17} /><span><b>{auth.student?.hostelBlock ?? 'Warden access'}</b><small>{auth.student?.hostelName ?? 'Hostel Help staff'}</small></span></div><button className="nav-link signout" onClick={logout}><LogOut size={18} /><span>Sign out</span></button></div>
    </aside>
    <main className="main-area">
      <header className="topbar"><button className="icon-button mobile-menu" aria-label="Open navigation" onClick={() => setMobileNav(!mobileNav)}><Menu size={20} /></button><div className="breadcrumb">HOSTEL HELP <ChevronRight size={14} /> <span>{nav.find((item) => item.id === page)?.label ?? 'Overview'}</span></div><div className="top-actions"><button className="icon-button" aria-label="Refresh data" title="Refresh data" onClick={refresh} disabled={busy}><RefreshCw size={17} className={busy ? 'spin' : ''} /></button>{!isWarden && <button className="icon-button notification-button" aria-label="Notifications" onClick={() => setPage('notifications')}><Bell size={18} />{notifications.some((item) => !item.read) && <i />}</button>}<div className="avatar">{auth.fullName.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase()}</div><div className="top-user"><b>{auth.fullName}</b><small>{isWarden ? 'Warden' : auth.student?.registrationNumber}</small></div></div></header>
      <div className="page-content">
        {error && <div className="alert alert-error" role="alert"><CircleAlert size={18} /><span>{error}</span><button aria-label="Dismiss" onClick={() => setError('')}><X size={16} /></button></div>}
        {isWarden ? <WardenReports auth={auth} overview={wardenOverview} reports={reports} reportDetails={reportDetails} expandedReportId={expandedReportId} loadingReportId={loadingReportId} onToggleReport={toggleReportDetails} onUpdate={(report, status) => runAction(() => api(`/api/warden/reports/${report.id}`, { method: 'PATCH', body: JSON.stringify({ status, resolutionNotes: status === 'RESOLVED' ? 'Reviewed by warden.' : '' }) }), 'Report updated.')} /> : <>
          {page === 'home' && <Dashboard auth={auth} requests={requests} mine={mine} borrowings={borrowings} offers={offers} setPage={setPage} onCreate={() => setModal('request')} onOffer={(item) => runAction(() => api(`/api/requests/${item.id}/offers`, { method: 'POST', body: JSON.stringify({ message: 'Happy to lend this to you.' }) }), 'Offer sent to the requester.')} />}
          {page === 'requests' && <RequestsPage requests={requests} search={search} setSearch={setSearch} urgency={urgency} setUrgency={setUrgency} onCreate={() => setModal('request')} onOffer={(item) => runAction(() => api(`/api/requests/${item.id}/offers`, { method: 'POST', body: JSON.stringify({ message: 'Happy to lend this to you.' }) }), 'Offer sent to the requester.')} />}
          {page === 'members' && <MembersPage members={members} search={memberSearch} setSearch={setMemberSearch} />}
          {page === 'offers' && <OffersPage offers={offers} studentId={auth.id} onAccept={(offer) => runAction(() => api(`/api/offers/${offer.id}/accept`, { method: 'POST' }), 'Offer accepted. A borrowing record is ready.')} onReject={(offer) => runAction(() => api(`/api/offers/${offer.id}/reject`, { method: 'POST' }), 'Offer declined.')} />}
          {page === 'borrowings' && <BorrowingsPage items={borrowings} studentId={auth.id} onReceived={(item) => runAction(() => api(`/api/borrowings/${item.id}/confirm-received`, { method: 'POST' }), 'Receipt confirmed.')} onReturn={(item) => runAction(() => api(`/api/borrowings/${item.id}/confirm-return`, { method: 'POST' }), item.borrowerId === auth.id ? 'Return reported. Waiting for the lender to confirm.' : 'Return confirmed.')} onRate={(item) => { setSelectedBorrowing(item); setModal('rating') }} onReport={(item) => { setSelectedBorrowing(item); setModal('report') }} />}
          {page === 'notifications' && <NotificationsPage notifications={notifications} onRead={(item) => runAction(() => api(`/api/notifications/${item.id}/read`, { method: 'PATCH' }), 'Notification marked as read.')} />}
          {page === 'profile' && <ProfilePage auth={auth} mine={mine} borrowings={borrowings} />}
        </>}
        {busy && <div className="loading-line"><span /> Updating your hostel space…</div>}
      </div>
    </main>
    {mobileNav && <button className="mobile-scrim" aria-label="Close navigation" onClick={() => setMobileNav(false)} />}
    {modal && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setModal(null) }}><section className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title"><div className="modal-head"><div><span className="eyebrow">HOSTEL HELP</span><h2 id="modal-title">{modal === 'request' ? 'Ask your neighbours' : modal === 'rating' ? 'Leave a thank-you' : 'Tell the warden'}</h2></div><button className="icon-button" aria-label="Close" onClick={() => setModal(null)}><X size={19} /></button></div>
      {modal === 'request' && <form className="form-grid" onSubmit={createRequest}><label>What do you need?<input name="itemName" required maxLength={120} placeholder="e.g. Phone charger" /></label><label>Category<select name="category" required><option value="">Choose a category</option><option>Electronics</option><option>Study supplies</option><option>Kitchen</option><option>Personal care</option><option>Other</option></select></label><label className="full-field">A little more detail<textarea name="description" required maxLength={2000} rows={3} placeholder="Add details that will help someone nearby." /></label><label>How soon?<select name="urgency"><option value="TODAY">Today</option><option value="NOW">As soon as possible</option><option value="THIS_WEEK">This week</option></select></label><label>Return by<input name="expectedReturnDate" type="date" required min={today} /></label><div className="full-field modal-actions"><button type="button" className="button button-quiet" onClick={() => setModal(null)}>Cancel</button><button className="button button-primary" disabled={busy}><Plus size={17} /> Post request</button></div></form>}
      {modal === 'rating' && <form className="form-grid" onSubmit={submitRating}><label>How was the help?<select name="score"><option value="5">5 - Wonderful</option><option value="4">4 - Helpful</option><option value="3">3 - Okay</option><option value="2">2 - Could be better</option><option value="1">1 - Not good</option></select></label><label className="full-field">A note (optional)<textarea name="comment" maxLength={1000} rows={3} placeholder="Say thanks or share a useful detail." /></label><div className="full-field modal-actions"><button type="button" className="button button-quiet" onClick={() => setModal(null)}>Cancel</button><button className="button button-primary" disabled={busy}><Star size={16} /> Send rating</button></div></form>}
      {modal === 'report' && <form className="form-grid" onSubmit={submitReport}><label className="full-field">What happened?<select name="reason"><option>Item not returned</option><option>Item damaged</option><option>Safety concern</option><option>Other issue</option></select></label><label className="full-field">Details<textarea name="description" required maxLength={3000} rows={4} placeholder="Give the warden enough context to review this." /></label><div className="full-field modal-actions"><button type="button" className="button button-quiet" onClick={() => setModal(null)}>Cancel</button><button className="button button-danger" disabled={busy}><FileWarning size={16} /> Send report</button></div></form>}
    </section></div>}
    {toast && <div className="toast" role="status"><Check size={17} />{toast}</div>}
  </div>
}

function AuthScreen({ mode, setMode, onSubmit, busy, error }: { mode: 'login' | 'register'; setMode: (mode: 'login' | 'register') => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void; busy: boolean; error: string }) {
  return <main className="auth-layout"><section className="auth-aside"><a className="brand brand-inverse" href="#"><span className="brand-mark"><HandHeart size={22} /></span><span>hostel<span className="brand-light">help</span><small>NEIGHBOURS, NOT STRANGERS</small></span></a><div className="auth-message"><span className="eyebrow">A SMALL ASK. A BIG DIFFERENCE.</span><h1>Help is closer than you think.</h1><p>Borrow what you need. Share what you have. Build a stronger hostel community.</p><div className="auth-note"><div className="note-icon"><HandHeart size={20} /></div><div><b>Built for the everyday moments</b><span>Chargers, notes, umbrellas and all the little things that get you through the day.</span></div></div></div><div className="auth-aside-foot">A kinder kind of campus living <span>01 / COMMUNITY</span></div></section>
    <section className="auth-panel"><div className="auth-form-wrap"><div className="auth-tabs"><button className={mode === 'login' ? 'selected' : ''} onClick={() => setMode('login')}>Sign in</button><button className={mode === 'register' ? 'selected' : ''} onClick={() => setMode('register')}>Create account</button></div><h2>{mode === 'login' ? 'Welcome back' : 'Join your hostel'}</h2><p className="auth-subtitle">{mode === 'login' ? 'Pick up where your community left off.' : 'Your next helpful neighbour is right here.'}</p>{error && <div className="alert alert-error"><CircleAlert size={17} /><span>{error}</span></div>}
      <form className="auth-form" onSubmit={onSubmit}>{mode === 'register' && <><label>Full name<input name="fullName" autoComplete="name" required maxLength={120} placeholder="Your name" /></label><label>Student registration number<input name="registrationNumber" required maxLength={50} placeholder="e.g. STU-2026-014" /></label></>}<label>Email address<input name="email" type="email" autoComplete="email" required placeholder="you@campus.edu" /></label><label>Password<input name="password" type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} minLength={10} required placeholder={mode === 'register' ? 'At least 10 characters' : 'Your password'} /></label>
        {mode === 'register' && <><label>Confirm password<input name="confirmPassword" type="password" autoComplete="new-password" minLength={10} required placeholder="Type it again" /></label><div className="field-row"><label>Hostel<input name="hostelName" required maxLength={120} placeholder="Hostel name" /></label><label>Block<input name="hostelBlock" required maxLength={80} placeholder="Block" /></label></div><label>Room number<input name="roomNumber" required maxLength={30} placeholder="Room" /></label></>}
        <button className="button button-primary auth-submit" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in to your space' : 'Create my account'}<ChevronRight size={17} /></button></form><p className="auth-privacy"><ShieldCheck size={15} /> Your account is for your hostel community.</p></div></section></main>
}

function Dashboard({ auth, requests, mine, borrowings, offers, setPage, onCreate, onOffer }: { auth: Auth; requests: RequestItem[]; mine: RequestItem[]; borrowings: Borrowing[]; offers: Offer[]; setPage: (page: Page) => void; onCreate: () => void; onOffer: (item: RequestItem) => void }) {
  const ongoing = borrowings.filter((item) => item.status !== 'RETURNED')
  const pending = offers.filter((offer) => offer.requesterId === auth.id && offer.status === 'PENDING')
  return <><section className="welcome-band"><div><span className="eyebrow">YOUR HOSTEL, YOUR PEOPLE</span><h1>Good to see you, {auth.fullName.split(' ')[0]}.</h1><p>A neighbour might have exactly what you need.</p></div><button className="button button-light" onClick={onCreate}><Plus size={17} /> Ask for an item</button><div className="welcome-doodle"><HandHeart size={85} strokeWidth={1} /></div></section>
    <div className="stats-row"><Stat label="Requests across all blocks" value={requests.length} note="Shared hostel-wide" icon={<Search size={18} />} tone="sage" /><Stat label="Your open requests" value={mine.filter((item) => ['OPEN', 'OFFERED'].includes(item.status)).length} note="Items you are looking for" icon={<BookOpen size={18} />} tone="coral" /><Stat label="Pending offers" value={pending.length} note="Ready for your decision" icon={<HandHeart size={18} />} tone="yellow" /><Stat label="Active borrowings" value={ongoing.length} note="Keep each other updated" icon={<ArrowDownUp size={18} />} tone="mint" /></div>
    <div className="section-heading"><div><span className="eyebrow">HOSTEL-WIDE</span><h2>Requests from every block</h2></div><button className="text-button" onClick={() => setPage('requests')}>See all <ChevronRight size={16} /></button></div>
    {requests.length ? <div className="request-grid">{requests.slice(0, 3).map((item) => <RequestCard key={item.id} item={item} onOffer={() => onOffer(item)} />)}</div> : <EmptyState icon={<HandHeart size={22} />} title="No requests in view" text="No one has posted a request yet. You can start one." />}
    <div className="section-heading activity-heading"><div><span className="eyebrow">KEEP IN TOUCH</span><h2>Your borrowing activity</h2></div><button className="text-button" onClick={() => setPage('borrowings')}>View activity <ChevronRight size={16} /></button></div><BorrowingList items={ongoing.slice(0, 3)} studentId={auth.id} compact />
  </>
}

function Stat({ label, value, note, icon, tone }: { label: string; value: number; note: string; icon: ReactNode; tone: string }) {
  return <div className="stat"><div className={`stat-icon ${tone}`}>{icon}</div><div><span>{label}</span><strong>{value}</strong><small>{note}</small></div></div>
}

function RequestsPage({ requests, search, setSearch, urgency, setUrgency, onCreate, onOffer }: { requests: RequestItem[]; search: string; setSearch: (value: string) => void; urgency: string; setUrgency: (value: string) => void; onCreate: () => void; onOffer: (request: RequestItem) => void }) {
  return <><PageIntro eyebrow="ASK AROUND" title="Borrowing starts anywhere." subtitle="Requests are shared across every hostel block." action={<button className="button button-primary" onClick={onCreate}><Plus size={17} /> New request</button>} /><div className="toolbar"><label className="search-box"><Search size={17} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search items or details" /></label><label className="select-box"><ArrowDownUp size={16} /><select value={urgency} onChange={(event) => setUrgency(event.target.value)}><option value="">Any urgency</option><option value="NOW">As soon as possible</option><option value="TODAY">Today</option><option value="THIS_WEEK">This week</option></select></label><span className="result-count">{requests.length} {requests.length === 1 ? 'request' : 'requests'}</span></div>{requests.length ? <div className="request-grid request-grid-wide">{requests.map((item) => <RequestCard key={item.id} item={item} onOffer={() => onOffer(item)} />)}</div> : <EmptyState icon={<Search size={22} />} title="Nothing matches just yet" text="Try a different search or urgency filter, or ask your neighbours for something." />}</>
}

function RequestCard({ item, onOffer }: { item: RequestItem; onOffer: () => void }) {
  const urgencyLabel = item.urgency === 'NOW' ? 'AS SOON AS POSSIBLE' : item.urgency === 'TODAY' ? 'TODAY' : 'THIS WEEK'
  return <article className="request-card"><div className="request-card-top"><div className="item-symbol"><BookOpen size={20} /></div><span className={`urgency urgency-${item.urgency.toLowerCase()}`}><i />{urgencyLabel}</span></div><h3>{item.itemName}</h3><span className="category-label">{item.category} · Block {item.hostelBlock}</span><p className="request-description">{item.description}</p><div className="request-meta"><span className="person-chip"><span className="mini-avatar">{item.requesterName.slice(0, 1)}</span>{item.requesterName}</span><span><Clock3 size={14} /> {new Date(item.createdAt).toLocaleDateString()}</span></div><div className="request-footer"><span>{item.offerCount} {item.offerCount === 1 ? 'offer' : 'offers'}</span><button className="button button-outline" onClick={onOffer}><HandHeart size={16} /> Offer help</button></div></article>
}

function MembersPage({ members, search, setSearch }: { members: Member[]; search: string; setSearch: (value: string) => void }) {
  const query = search.trim().toLowerCase()
  const visible = members.filter((member) => `${member.fullName} ${member.hostelName} ${member.hostelBlock} ${member.roomNumber}`.toLowerCase().includes(query))
  return <><PageIntro eyebrow="HOSTEL COMMUNITY" title="Meet your neighbours" subtitle="Find members across all blocks by name, hostel, or room." /><div className="toolbar"><label className="search-box"><Search size={17} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search members or blocks" /></label><span className="result-count">{visible.length} {visible.length === 1 ? 'member' : 'members'}</span></div>{visible.length ? <div className="member-grid">{visible.map((member) => <article className="member-row" key={member.id}><span className="profile-avatar">{member.fullName.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase()}</span><div className="member-main"><b>{member.fullName}</b><span>{member.hostelName}</span></div><div className="member-location"><span>BLOCK</span><b>{member.hostelBlock}</b></div><div className="member-location"><span>ROOM</span><b>{member.roomNumber}</b></div></article>)}</div> : <EmptyState icon={<Users size={22} />} title="No members found" text="Try another name, block, or room search." />}</>
}

function OffersPage({ offers, studentId, onAccept, onReject }: { offers: Offer[]; studentId: number; onAccept: (offer: Offer) => void; onReject: (offer: Offer) => void }) {
  return <><PageIntro eyebrow="GOOD THINGS IN MOTION" title="Offers & helpers" subtitle="Choose an offer that feels right, or follow offers you have made." />{offers.length ? <div className="list-panel">{offers.map((offer, index) => {
    const incoming = offer.requesterId === studentId
    return <article className="activity-row" key={`${offer.id}-${incoming ? 'in' : `out-${index}`}`}><div className={`activity-icon ${incoming ? 'activity-in' : 'activity-out'}`}><HandHeart size={19} /></div><div className="activity-main"><div className="activity-title"><b>{offer.itemName}</b><span className={`status-pill status-${offer.status.toLowerCase()}`}>{offer.status}</span></div><p>{incoming ? `${offer.helperName} offered to help with your request` : `Your offer for ${offer.itemName}`}{offer.message ? ` · “${offer.message}”` : ''}</p><small>{new Date(offer.offeredAt).toLocaleString()}</small></div>{incoming && offer.status === 'PENDING' && <div className="row-actions"><button className="button button-primary button-small" onClick={() => onAccept(offer)}><Check size={15} /> Accept</button><button className="icon-button" title="Decline offer" aria-label="Decline offer" onClick={() => onReject(offer)}><X size={17} /></button></div>}</article>
  })}</div> : <EmptyState icon={<HandHeart size={22} />} title="No offers yet" text="When someone offers to lend an item, it will show up here." />}</>
}

function BorrowingsPage({ items, studentId, onReceived, onReturn, onRate, onReport }: { items: Borrowing[]; studentId: number; onReceived: (item: Borrowing) => void; onReturn: (item: Borrowing) => void; onRate: (item: Borrowing) => void; onReport: (item: Borrowing) => void }) {
  return <><PageIntro eyebrow="BORROWING, MADE SIMPLE" title="Items in the loop" subtitle="A clear record helps both neighbours feel at ease." /><BorrowingList items={items} studentId={studentId} onReceived={onReceived} onReturn={onReturn} onRate={onRate} onReport={onReport} /></>
}

function BorrowingList({ items, studentId, onReceived, onReturn, onRate, onReport }: { items: Borrowing[]; studentId: number; compact?: boolean; onReceived?: (item: Borrowing) => void; onReturn?: (item: Borrowing) => void; onRate?: (item: Borrowing) => void; onReport?: (item: Borrowing) => void }) {
  if (!items.length) return <EmptyState icon={<ArrowDownUp size={22} />} title="No active borrowings" text="When an offer is accepted, the borrowing timeline will appear here." />
  return <div className="list-panel">{items.map((item) => {
    const borrower = item.borrowerId === studentId
    let action: ReactNode = null
    if (item.status === 'AWAITING_RECEIPT' && borrower && onReceived) action = <button className="button button-primary button-small" onClick={() => onReceived(item)}><Check size={15} /> Confirm received</button>
    if (item.status === 'BORROWED' && borrower && !item.returnInitiatedAt && onReturn) action = <button className="button button-outline button-small" onClick={() => onReturn(item)}>I returned it</button>
    if (item.status === 'BORROWED' && !borrower && item.returnInitiatedAt && onReturn) action = <button className="button button-primary button-small" onClick={() => onReturn(item)}><Check size={15} /> Confirm return</button>
    if (item.status === 'RETURNED' && borrower && onRate) action = <div className="row-actions">{!item.rated && <button className="button button-outline button-small" onClick={() => onRate(item)}><Star size={14} /> Rate helper</button>}{item.rated && <span className="status-pill status-returned">RATED</span>}{onReport && <button className="icon-button" title="Report a problem" aria-label="Report a problem" onClick={() => onReport(item)}><FileWarning size={17} /></button>}</div>
    return <article className="activity-row borrowing-row" key={item.id}><div className={`activity-icon ${item.status === 'RETURNED' ? 'activity-done' : 'activity-in'}`}><ArrowDownUp size={18} /></div><div className="activity-main"><div className="activity-title"><b>{item.itemName}</b><span className={`status-pill status-${item.status.toLowerCase()}`}>{item.status.replaceAll('_', ' ')}</span></div><p>{borrower ? `Lent by ${item.helperName}` : `Borrowed by ${item.borrowerName}`}{item.expectedReturnDate ? ` · Due ${new Date(`${item.expectedReturnDate}T00:00:00`).toLocaleDateString()}` : ''}</p><small>{item.returnInitiatedAt && !item.returnedAt ? 'Return reported; waiting for lender confirmation' : item.receivedAt ? `Received ${new Date(item.receivedAt).toLocaleDateString()}` : 'Waiting for receipt confirmation'}</small></div>{action}</article>
  })}</div>
}

function NotificationsPage({ notifications, onRead }: { notifications: Notification[]; onRead: (item: Notification) => void }) {
  return <><PageIntro eyebrow="THE LITTLE UPDATES" title="Notifications" subtitle="Stay in step with your hostel community." />{notifications.length ? <div className="list-panel">{notifications.map((item) => <article className={`activity-row ${item.read ? 'row-read' : ''}`} key={item.id}><div className="activity-icon activity-note"><Bell size={17} /></div><div className="activity-main"><div className="activity-title"><b>{item.type.replaceAll('_', ' ').toLowerCase()}</b>{!item.read && <span className="unread-dot" />}</div><p>{item.message}</p><small>{new Date(item.createdAt).toLocaleString()}</small></div>{!item.read && <button className="icon-button" title="Mark as read" aria-label="Mark as read" onClick={() => onRead(item)}><CheckCheck size={17} /></button>}</article>)}</div> : <EmptyState icon={<Bell size={21} />} title="All caught up" text="When something needs your attention, you will find it here." />}</>
}

function ProfilePage({ auth, mine, borrowings }: { auth: Auth; mine: RequestItem[]; borrowings: Borrowing[] }) {
  const student = auth.student
  if (!student) return null
  return <><PageIntro eyebrow="YOUR COMMUNITY CARD" title="Your profile" subtitle="A little context helps neighbours feel comfortable lending." /><div className="profile-layout"><section className="profile-main"><div className="profile-banner"><div className="profile-avatar">{auth.fullName.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase()}</div><div><span className="eyebrow">STUDENT MEMBER</span><h2>{student.fullName}</h2><p>{student.hostelName} · Block {student.hostelBlock} · Room {student.roomNumber}</p></div></div><div className="profile-fields"><div><span>Email</span><b>{student.email}</b></div><div><span>Registration number</span><b>{student.registrationNumber}</b></div><div><span>Hostel</span><b>{student.hostelName}</b></div><div><span>Room</span><b>{student.roomNumber}</b></div></div></section><aside className="profile-stats"><div className="trust-mark"><ShieldCheck size={23} /><span>COMMUNITY TRUST</span><strong>{Number(student.trustScore).toFixed(1)}<small> / 5</small></strong></div><div className="profile-stat"><span>Requests posted</span><b>{mine.length}</b></div><div className="profile-stat"><span>Borrowings joined</span><b>{borrowings.length}</b></div></aside></div></>
}

function WardenReports({ auth, overview, reports, reportDetails, expandedReportId, loadingReportId, onToggleReport, onUpdate }: { auth: Auth; overview: WardenOverview | null; reports: Report[]; reportDetails: Record<number, WardenReportDetails>; expandedReportId: number | null; loadingReportId: number | null; onToggleReport: (reportId: number) => void; onUpdate: (report: Report, status: string) => void }) {
  return <>
    <PageIntro eyebrow="WARDEN WORKSPACE" title="Hostel activity and reports" subtitle={`Review activity across all blocks.${auth.wardenPhone ? ` Contact: ${auth.wardenPhone}.` : ''}`} />
    <div className="warden-summary">
      <div><Users size={20} /><span>Members</span><b>{overview?.memberCount ?? 0}</b></div>
      <div><Search size={20} /><span>Requests</span><b>{overview?.requestCount ?? 0}</b></div>
      <div><HandHeart size={20} /><span>Offers</span><b>{overview?.offerCount ?? 0}</b></div>
      <div><ArrowDownUp size={20} /><span>Borrowings</span><b>{overview?.borrowingCount ?? 0}</b></div>
      <div><Star size={20} /><span>Ratings</span><b>{overview?.ratingCount ?? 0}</b></div>
      <div><Bell size={20} /><span>Notifications</span><b>{overview?.notificationCount ?? 0}</b></div>
      <div><FileWarning size={20} /><span>Open reports</span><b>{reports.filter((report) => report.status === 'OPEN').length}</b></div>
    </div>
    <section className="warden-activity">
      <h2>All members</h2>
      {overview?.members.length ? <div className="list-panel warden-member-list">{overview.members.map((member) => <article className="warden-member-row" key={member.id}>
        <div className="member-identity"><span className="mini-avatar">{member.fullName.slice(0, 1)}</span><div><b>{member.fullName}</b><small>{member.email}</small></div></div>
        <div className="member-admin-detail"><span>REGISTRATION</span><b>{member.registrationNumber}</b></div>
        <div className="member-admin-detail"><span>HOSTEL / BLOCK</span><b>{member.hostelName} · {member.hostelBlock}</b></div>
        <div className="member-admin-detail"><span>ROOM</span><b>{member.roomNumber}</b></div>
        <div className="member-admin-detail"><span>TRUST</span><b>{Number(member.trustScore).toFixed(1)} / 5</b></div>
        <span className={`status-pill ${member.enabled ? 'status-returned' : 'status-dismissed'}`}>{member.enabled ? 'ACTIVE' : 'DISABLED'}</span>
      </article>)}</div> : <EmptyState icon={<Users size={20} />} title="No members yet" text="Registered hostel members will appear here." />}
    </section>
    <section className="warden-activity">
      <h2>Recent requests</h2>
      {overview?.recentRequests.length ? <div className="list-panel">{overview.recentRequests.map((item) => <article className="activity-row" key={item.id}>
        <div className="activity-icon"><BookOpen size={18} /></div><div className="activity-main"><div className="activity-title"><b>{item.itemName}</b><span className="status-pill">Block {item.hostelBlock}</span></div><p>{item.requesterName} · {item.category} · {item.status.replaceAll('_', ' ')}</p><small>{new Date(item.createdAt).toLocaleString()}</small></div>
      </article>)}</div> : <EmptyState icon={<BookOpen size={20} />} title="No requests yet" text="Requests from every block will appear here." />}
    </section>
    <section className="warden-activity">
      <h2>Recent offers and borrowings</h2>
      {overview && (overview.recentOffers.length > 0 || overview.recentBorrowings.length > 0) ? <div className="list-panel">
        {overview.recentOffers.slice(0, 5).map((offer) => <article className="activity-row" key={`offer-${offer.id}`}>
          <div className="activity-icon activity-out"><HandHeart size={18} /></div><div className="activity-main"><div className="activity-title"><b>{offer.itemName}</b><span className={`status-pill status-${offer.status.toLowerCase()}`}>{offer.status}</span></div><p>{offer.helperName} offered to help · Request #{offer.requestId}</p><small>{new Date(offer.offeredAt).toLocaleString()}</small></div>
        </article>)}
        {overview.recentBorrowings.slice(0, 5).map((item) => <article className="activity-row" key={`borrowing-${item.id}`}>
          <div className="activity-icon activity-done"><ArrowDownUp size={18} /></div><div className="activity-main"><div className="activity-title"><b>{item.itemName}</b><span className={`status-pill status-${item.status.toLowerCase()}`}>{item.status.replaceAll('_', ' ')}</span></div><p>{item.borrowerName} borrowed from {item.helperName}</p><small>{item.receivedAt ? new Date(item.receivedAt).toLocaleString() : `Due ${item.expectedReturnDate}`}</small></div>
        </article>)}
      </div> : <EmptyState icon={<ArrowDownUp size={20} />} title="No activity yet" text="Offers and borrowing records will appear here." />}
    </section>
    <section className="warden-activity">
      <h2>Incident reports</h2>
      {reports.length ? <div className="report-grid">{reports.map((report) => <article className={`report-card ${expandedReportId === report.id ? 'report-card-expanded' : ''}`} key={report.id}>
        <div className="report-card-head"><span className="report-symbol"><FileWarning size={19} /></span><span className={`status-pill status-${report.status.toLowerCase()}`}>{report.status.replaceAll('_', ' ')}</span></div>
        <button className="report-expand-trigger" aria-expanded={expandedReportId === report.id} onClick={() => onToggleReport(report.id)}>
          <span className="eyebrow">{report.itemName} · TRANSACTION #{report.borrowingTransactionId}</span><h3>{report.reason}</h3><p>{report.description}</p>
          <span className="report-by">Reported by <b>{report.reporterName}</b> · {new Date(report.createdAt).toLocaleDateString()}</span>
          <span className="report-expand-label">{expandedReportId === report.id ? 'Hide full details' : 'View full details'} <ChevronRight size={15} /></span>
        </button>
        {expandedReportId === report.id && <ReportDetailPanel details={reportDetails[report.id]} loading={loadingReportId === report.id} />}
        <label className="report-status">Update status<select value={report.status} onChange={(event) => onUpdate(report, event.target.value)}><option value="OPEN">Open</option><option value="UNDER_REVIEW">Under review</option><option value="RESOLVED">Resolved</option><option value="DISMISSED">Dismissed</option></select></label>
      </article>)}</div> : <EmptyState icon={<ShieldCheck size={22} />} title="No reports to review" text="Student-submitted incidents will appear here." />}
    </section>
    <section className="warden-activity">
      <h2>Recent ratings</h2>
      {overview?.recentRatings.length ? <div className="list-panel">{overview.recentRatings.map((rating) => <article className="activity-row" key={rating.id}>
        <div className="activity-icon activity-note"><Star size={18} /></div><div className="activity-main"><div className="activity-title"><b>{rating.score} / 5 · {rating.helperName}</b></div><p>Rated by {rating.borrowerName}{rating.comment ? ` · ${rating.comment}` : ''}</p><small>{new Date(rating.createdAt).toLocaleString()}</small></div>
      </article>)}</div> : <EmptyState icon={<Star size={20} />} title="No ratings yet" text="Ratings will appear here after borrowings are completed." />}
    </section>
  </>
}

function ReportDetailPanel({ details, loading }: { details?: WardenReportDetails; loading: boolean }) {
  if (loading) return <div className="report-detail-loading">Loading linked member and borrowing details…</div>
  if (!details) return null
  const people = [
    { label: 'Reporter', member: details.reporter },
    { label: 'Borrower', member: details.borrower },
    { label: 'Helper', member: details.helper },
  ]
  return <div className="report-detail-panel">
    <div className="report-detail-section"><h4>People involved</h4><div className="report-people-grid">{people.map(({ label, member }) => <div className="report-detail-person" key={label}>
      <span>{label}</span><b>{member.fullName}</b><small>{member.email} · {member.registrationNumber}</small><small>{member.hostelName}, Block {member.hostelBlock}, Room {member.roomNumber}</small>
    </div>)}</div></div>
    <div className="report-detail-section"><h4>Original request</h4><div className="report-detail-fields">
      <div><span>Item</span><b>{details.request.itemName}</b></div><div><span>Category</span><b>{details.request.category}</b></div><div><span>Request status</span><b>{details.request.status.replaceAll('_', ' ')}</b></div><div><span>Urgency</span><b>{details.request.urgency.replaceAll('_', ' ')}</b></div><div><span>Request block</span><b>{details.request.hostelBlock}</b></div><div><span>Expected return</span><b>{details.request.expectedReturnDate ?? 'Not set'}</b></div>
      <div className="report-detail-wide"><span>Description</span><p>{details.request.description}</p></div><div><span>Posted</span><b>{new Date(details.request.createdAt).toLocaleString()}</b></div>
    </div></div>
    <div className="report-detail-section"><h4>Offer and borrowing timeline</h4><div className="report-detail-fields">
      <div><span>Offer status</span><b>{details.offer.status}</b></div><div><span>Offered</span><b>{new Date(details.offer.offeredAt).toLocaleString()}</b></div><div><span>Accepted</span><b>{details.offer.acceptedAt ? new Date(details.offer.acceptedAt).toLocaleString() : 'Not accepted'}</b></div><div><span>Borrowing status</span><b>{details.borrowing.status.replaceAll('_', ' ')}</b></div><div><span>Receipt confirmed</span><b>{details.borrowing.receivedAt ? new Date(details.borrowing.receivedAt).toLocaleString() : 'No'}</b></div><div><span>Return reported</span><b>{details.borrowing.returnInitiatedAt ? new Date(details.borrowing.returnInitiatedAt).toLocaleString() : 'No'}</b></div><div><span>Return confirmed</span><b>{details.borrowing.returnedAt ? new Date(details.borrowing.returnedAt).toLocaleString() : 'No'}</b></div>
      {details.offer.message && <div className="report-detail-wide"><span>Offer message</span><p>{details.offer.message}</p></div>}
    </div></div>
    {details.report.resolutionNotes && <div className="report-detail-section"><h4>Resolution notes</h4><p>{details.report.resolutionNotes}</p></div>}
  </div>
}

function PageIntro({ eyebrow, title, subtitle, action }: { eyebrow: string; title: string; subtitle: string; action?: ReactNode }) {
  return <div className="page-intro"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{subtitle}</p></div>{action}</div>
}

function EmptyState({ icon, title, text }: { icon: ReactNode; title: string; text: string }) {
  return <section className="empty-state"><span className="empty-icon">{icon}</span><h3>{title}</h3><p>{text}</p></section>
}

export default App
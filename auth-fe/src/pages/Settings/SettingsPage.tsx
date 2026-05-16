import PageMeta from "../../components/common/PageMeta";
import Badge from "../../components/ui/badge/Badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { BoltIcon, CheckCircleIcon, LockIcon } from "../../icons";

// ── Static config data ────────────────────────────────────────────────────────

const JWT_CONFIG = [
  { label: "Issuer", value: "auth-service", mono: true },
  { label: "Audience", value: "shop-platform", mono: true },
  { label: "Access TTL", value: "15 minutes", mono: true },
  { label: "Refresh TTL", value: "7 days", mono: true },
  { label: "Algorithm", value: "HS512", mono: true },
];

const JWT_CLAIMS = ["userId", "role", "tokenType", "groups", "permissions"];

const SECURITY_CONFIG = [
  {
    label: "MFA",
    node: (
      <Badge color="success" size="sm">
        Required for STAFF / OVERSIGHT / ADMIN
      </Badge>
    ),
  },
  {
    label: "Lockout",
    node: <span className="text-sm text-gray-700 dark:text-gray-300">5 failed attempts · 30-min lock</span>,
  },
  {
    label: "Password policy",
    node: <span className="text-sm text-gray-700 dark:text-gray-300">NIST 800-63B · min 12 chars · breach check</span>,
  },
  {
    label: "Method security",
    node: (
      <span className="inline-flex items-center gap-1 rounded-md border border-brand-200 bg-brand-50 dark:border-brand-500/30 dark:bg-brand-500/10 px-2 py-0.5 text-xs font-mono text-brand-700 dark:text-brand-300">
        @EnableMethodSecurity
      </span>
    ),
  },
  {
    label: "Session",
    node: (
      <span className="inline-flex items-center gap-1 rounded-md border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 px-2 py-0.5 text-xs font-mono text-gray-600 dark:text-gray-400">
        STATELESS
      </span>
    ),
  },
  {
    label: "Default group",
    node: (
      <span className="inline-flex items-center gap-1 rounded-md border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 px-2 py-0.5 text-xs font-mono text-gray-600 dark:text-gray-400">
        RETAIL_CUSTOMER
      </span>
    ),
  },
];

const COMPLIANCE_STANDARDS = [
  {
    standard: "PCI-DSS v4",
    reqs: ["Req 7.2 — Least privilege", "Req 8.6 — MFA", "Req 10.2 — Audit log"],
  },
  {
    standard: "NIST SP 800-63B",
    reqs: ["Password length ≥ 12", "Breach corpus check", "No periodic forced rotation"],
  },
  {
    standard: "ISO 27001",
    reqs: ["A.9.4.2 — Separation of duties", "A.12.4 — Logging", "A.9.2 — Access provisioning"],
  },
  {
    standard: "OWASP ASVS L2",
    reqs: ["V2 — Authentication", "V4 — Access control", "V7 — Error handling & logging"],
  },
];

const MIGRATIONS = [
  { id: 1, version: "V10", name: "create_rbac_tables", applied: "2026-04-28 11:02:14" },
  { id: 2, version: "V11", name: "seed_rbac_banking_data", applied: "2026-04-28 11:02:15" },
  { id: 3, version: "V12", name: "backfill_user_groups", applied: "2026-05-01 09:14:00" },
  { id: 4, version: "V13", name: "deprecate_role_column", applied: "2026-05-02 03:00:00" },
];

// ── Sub-components ────────────────────────────────────────────────────────────

function ConfigRow({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="grid grid-cols-[160px_1fr] items-start border-b border-gray-100 dark:border-gray-800 last:border-0 px-5 py-3">
      <dt className="text-xs font-semibold uppercase tracking-wide text-gray-400 dark:text-gray-500 pt-0.5">
        {label}
      </dt>
      <dd>{children}</dd>
    </div>
  );
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function SettingsPage() {
  return (
    <>
      <PageMeta
        title="Settings | Auth Platform"
        description="Auth service configuration — JWT, security policy, and database migrations"
      />

      {/* ── Page header ── */}
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-800 dark:text-white/90">
          Settings
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Auth service configuration
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">

        {/* ── JWT Configuration ── */}
        <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
          <div className="flex items-center gap-3 border-b border-gray-100 dark:border-gray-800 px-5 py-4">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-50 dark:bg-brand-500/15">
              <BoltIcon className="size-5 text-brand-500" />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
                JWT configuration
              </h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Token issuance and claim settings
              </p>
            </div>
          </div>
          <dl className="py-2">
            {JWT_CONFIG.map(({ label, value, mono }) => (
              <ConfigRow key={label} label={label}>
                {mono ? (
                  <span className="inline-flex items-center rounded-md border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 px-2 py-0.5 text-xs font-mono text-gray-700 dark:text-gray-300">
                    {value}
                  </span>
                ) : (
                  <span className="text-sm text-gray-700 dark:text-gray-300">{value}</span>
                )}
              </ConfigRow>
            ))}
            <ConfigRow label="Embedded claims">
              <div className="flex flex-wrap gap-1.5">
                {JWT_CLAIMS.map((claim) => (
                  <span
                    key={claim}
                    className="inline-flex items-center rounded-md border border-brand-200 bg-brand-50 dark:border-brand-500/30 dark:bg-brand-500/10 px-2 py-0.5 text-xs font-mono text-brand-700 dark:text-brand-300"
                  >
                    {claim}
                  </span>
                ))}
              </div>
            </ConfigRow>
          </dl>
        </div>

        {/* ── Security Policy ── */}
        <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
          <div className="flex items-center gap-3 border-b border-gray-100 dark:border-gray-800 px-5 py-4">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-warning-50 dark:bg-warning-500/15">
              <LockIcon className="size-5 text-warning-500" />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
                Security policy
              </h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Authentication and access control rules
              </p>
            </div>
          </div>
          <dl className="py-2">
            {SECURITY_CONFIG.map(({ label, node }) => (
              <ConfigRow key={label} label={label}>
                {node}
              </ConfigRow>
            ))}
          </dl>
        </div>

        {/* ── Compliance Standards ── */}
        <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
          <div className="border-b border-gray-100 dark:border-gray-800 px-5 py-4">
            <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
              Compliance standards
            </h3>
            <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
              Frameworks this implementation targets
            </p>
          </div>
          <div className="p-5 space-y-3">
            {COMPLIANCE_STANDARDS.map(({ standard, reqs }) => (
              <div
                key={standard}
                className="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden"
              >
                <div className="bg-gray-50 dark:bg-white/[0.02] px-4 py-2.5 border-b border-gray-100 dark:border-gray-800">
                  <span className="text-xs font-semibold text-gray-700 dark:text-gray-300">
                    {standard}
                  </span>
                </div>
                <ul className="divide-y divide-gray-100 dark:divide-gray-800">
                  {reqs.map((req) => (
                    <li
                      key={req}
                      className="flex items-center gap-2.5 px-4 py-2"
                    >
                      <CheckCircleIcon className="size-3.5 text-success-500 shrink-0" />
                      <span className="text-xs text-gray-600 dark:text-gray-400">{req}</span>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>

        {/* ── Database Migrations ── */}
        <div className="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] overflow-hidden">
          <div className="border-b border-gray-100 dark:border-gray-800 px-5 py-4">
            <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">
              Database migrations
            </h3>
            <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
              Flyway migration history
            </p>
          </div>
          <Table>
            <TableHeader>
              <TableRow className="border-b border-gray-100 dark:border-gray-800">
                {["Version", "Name", "Applied", "Status"].map((h) => (
                  <TableCell
                    key={h}
                    isHeader
                    className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400"
                  >
                    {h}
                  </TableCell>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody className="divide-y divide-gray-100 dark:divide-gray-800">
              {MIGRATIONS.map((m) => (
                <TableRow
                  key={m.id}
                  className="hover:bg-gray-50 dark:hover:bg-white/[0.02] transition-colors"
                >
                  <TableCell className="px-5 py-3">
                    <span className="inline-flex items-center rounded-md border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 px-2 py-0.5 text-xs font-mono font-semibold text-gray-700 dark:text-gray-300">
                      {m.version}
                    </span>
                  </TableCell>
                  <TableCell className="px-5 py-3 text-xs font-mono text-gray-700 dark:text-gray-300">
                    {m.name}
                  </TableCell>
                  <TableCell className="px-5 py-3 text-xs font-mono text-gray-500 dark:text-gray-400 whitespace-nowrap">
                    {m.applied}
                  </TableCell>
                  <TableCell className="px-5 py-3">
                    <Badge color="success" size="sm">SUCCESS</Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

      </div>
    </>
  );
}

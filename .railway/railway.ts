import { defineRailway, github, postgres, project, service } from "railway/iac";

const REPOSITORIO = "Artur-Bertoni/Unidos-Pelo-Volei";

const codigo = (pasta: string) => github(REPOSITORIO, { branch: "main", rootDirectory: pasta });

const dockerfile = (pasta: string) => ({
  builder: "DOCKERFILE" as const,
  dockerfilePath: "Dockerfile",
  watchPatterns: [`/${pasta}/**`],
});

export default defineRailway(() => {
  const banco = postgres("Postgres");

  const segredosDoServidor = {
    DATABASE_URL: "${{Postgres.DATABASE_URL}}",
    JWT_SECRET: "${{shared.JWT_SECRET}}",
    GOOGLE_CLIENT_IDS: "${{shared.GOOGLE_WEB_CLIENT_ID}}",
    VAPID_PUBLIC_KEY: "${{shared.VAPID_PUBLIC_KEY}}",
    VAPID_PRIVATE_KEY: "${{shared.VAPID_PRIVATE_KEY}}",
    FIREBASE_SERVICE_ACCOUNT: "${{shared.FIREBASE_SERVICE_ACCOUNT}}",
  };

  const api = service("api", {
    source: codigo("server"),
    build: dockerfile("server"),
    deploy: {
      startCommand: "node dist/index.js",
      healthcheckPath: "/saude",
      healthcheckTimeout: 300,
      restartPolicyType: "ON_FAILURE",
      restartPolicyMaxRetries: 10,
    },
    env: {
      ...segredosDoServidor,
      PORT: "8080",
    },
  });

  const avisos = service("avisos", {
    source: codigo("server"),
    build: dockerfile("server"),
    deploy: {
      startCommand: "node dist/avisos.js",
      cronSchedule: "0 11,22 * * 5,6",
      restartPolicyType: "NEVER",
    },
    env: segredosDoServidor,
  });

  const powersync = service("powersync", {
    source: codigo("powersync"),
    build: dockerfile("powersync"),
    deploy: {
      healthcheckPath: "/probes/liveness",
      healthcheckTimeout: 300,
      restartPolicyType: "ALWAYS",
    },
    env: {
      PORT: "8080",
      PS_PORT: "8080",
      PS_DATA_SOURCE_URI: "${{Postgres.DATABASE_URL}}",
      PS_STORAGE_URI:
        "postgresql://${{Postgres.PGUSER}}:${{Postgres.PGPASSWORD}}@${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/powersync_storage",
      PS_JWT_SECRET: "${{shared.JWT_SECRET}}",
    },
  });

  const web = service("web", {
    source: codigo("web"),
    build: dockerfile("web"),
    deploy: {
      healthcheckPath: "/",
      restartPolicyType: "ON_FAILURE",
    },
    env: {
      PORT: "8080",
      VITE_API_URL: "https://${{api.RAILWAY_PUBLIC_DOMAIN}}",
      VITE_POWERSYNC_URL: "https://${{powersync.RAILWAY_PUBLIC_DOMAIN}}",
      VITE_GOOGLE_CLIENT_ID: "${{shared.GOOGLE_WEB_CLIENT_ID}}",
      VITE_VAPID_PUBLIC_KEY: "${{shared.VAPID_PUBLIC_KEY}}",
      VITE_ENVIRONMENT: "prod",
    },
  });

  return project("unidos-pelo-volei", {
    environments: ["production"],
    resources: [banco, api, avisos, powersync, web],
  });
});

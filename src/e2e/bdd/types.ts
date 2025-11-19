import "dotenv/config";
import z from "zod";

const userSchema = z.object({
  username: z.string(),
  password: z.string(),
});
export type User = z.infer<typeof userSchema>;

const envSchema = z.object({
  businessLanguage: z.string().refine((val) => ["en"].includes(val)),
  baseUrl: z.url(),
  users: z.record(z.string(), userSchema),
  caseTypes: z.record(z.string(), z.string()),
});
export type Env = z.infer<typeof envSchema>;

export const DEFAULT_USER = "DEFAULT_USER";

export const ENV = envSchema.parse({
  businessLanguage: process.env.BUSINESS_LANGUAGE ?? "en",
  baseUrl: process.env.ZAC_URL,
  users: {
    [DEFAULT_USER]: {
      username: process.env.BEHEERDER_USERNAME,
      password: process.env.BEHEERDER_PASSWORD,
    },
    beheerder: {
      username: process.env.BEHEERDER_USERNAME,
      password: process.env.BEHEERDER_PASSWORD,
    },
    thisuserdoesnotexist: {
      username: process.env.NON_EXISTING_USER_USERNAME,
      password: process.env.NON_EXISTING_USER_PASSWORD,
    },
  },
  caseTypes: {
    CMMN: process.env.CMMN_CASE_TYPE,
    BPMN: process.env.BPMN_CASE_TYPE,
  },
} satisfies Env);

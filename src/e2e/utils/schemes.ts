import {z} from 'zod'
import {ICreateAttachment} from '@cucumber/cucumber/lib/runtime/attachment_manager'

export const worldPossibleZacUrls = z.enum(['zac'])
export const worldUsers = z.enum(['Bob'])

export const worldParametersScheme = z.object({
    attach: z.any().refine((val): val is ICreateAttachment => {
        return typeof val === 'function'
    }),
    log: z.function(),
    parameters: z.object({
        urls: z.object({
            zac: z.string(),
            openForms: z.string().optional(),
        }),
        users: z.object({
            [worldUsers.Values.Bob]: z.object({
                username: z.string(),
                password: z.string(),
            }),
        }),
        headless: z.boolean(),
    })
})


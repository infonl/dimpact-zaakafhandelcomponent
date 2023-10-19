import {z} from 'zod'
import {ICreateAttachment} from '@cucumber/cucumber/lib/runtime/attachment_manager'

export const worldParametersScheme = z.object({
    attach: z.any().refine((val): val is ICreateAttachment => {
        return typeof val === 'function'
    }),
    log: z.function(),
    parameters: z.object({
        urls: z.object({
            zac: z.string().url(),
        }),
    })
})

export const worldPossibleZacUrls = z.enum(['zac'])
import { describe, test } from 'node:test';
import { Pubsub } from '../src/pubsub';

describe("site.pubsub 'after' and 'complete' methods", () => {
  test('after then complete', async () => {
    const pubsub = new Pubsub();
    const event = 'socket.hasConnected';
    const promise = pubsub.after(event);
    pubsub.complete(event);
    await promise;
  });

  test('complete then after', async () => {
    const pubsub = new Pubsub();
    const event = 'socket.hasConnected';
    pubsub.complete(event);
    const promise = pubsub.after(event);
    await promise;
  });

  test('one completion is never enough', async () => {
    const pubsub = new Pubsub();
    const event = 'socket.hasConnected';
    const await1 = pubsub.after(event);
    pubsub.complete(event);
    pubsub.complete(event);
    const await2 = pubsub.after(event);
    pubsub.complete(event);
    await await1;
    await await2;
  });
});

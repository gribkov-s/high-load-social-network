package com.sgribkov.socialnetwork.repository.userpostfeed

import com.sgribkov.socialnetwork.data.entities.UserId
import com.sgribkov.socialnetwork.data.entities.post.PostMessage
import zio.{Task, UIO, ZIO}
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import io.circe.syntax._
import cats.implicits._
import com.sgribkov.socialnetwork.data.Error.{CacheError, CanNotInvalidatePostFeed}
import com.sgribkov.socialnetwork.data.dto.PostFeedMessageDTO
import zio.interop.catz._
import com.sgribkov.socialnetwork.system.config.CacheConfig
import dev.profunktor.redis4cats.effect.Log.NoOp.instance
import dev.profunktor.redis4cats.effects.{Score, ScoreWithValue}


class UserPostFeedCache(config: CacheConfig) extends UserPostFeedRepo.Service {

  private def execCommand[T](conf: CacheConfig,
                             cmd: RedisCommands[Task, String, String] => Task[T]
                            ): Task[T] =
    Redis[Task].utf8(conf.uri).use(redis => cmd(redis))

  override def insert(subscriber: UserId, post: PostMessage, reduceFeedTo: Int): Task[Boolean] = {
    val cmd: RedisCommands[Task, String, String] => Task[Boolean] = redis =>
      for {
        elem <- ZIO.succeed(
          ScoreWithValue(
            Score(post.getTimeDouble),
            PostFeedMessageDTO.from(post).asJson.toString
          )
        )
        added <- redis.zAdd(subscriber.value, None, elem)
          .mapError(e => CacheError(e.getMessage))
        reduce = redis.zRemRangeByRank(subscriber.value, 0, - reduceFeedTo - 1)
          .mapError(e => CacheError(e.getMessage))
        _ <- if (reduceFeedTo > 0) reduce else ZIO.unit
        res = if (added == 0) false else true
      } yield res
    execCommand[Boolean](config, cmd)
  }

  override def delete(subscriber: UserId, post: PostMessage): Task[Boolean] = {
    val cmd: RedisCommands[Task, String, String] => Task[Boolean] = redis =>
      for {
        deleted <- redis.zRem(
          subscriber.value,
          PostFeedMessageDTO.from(post).asJson.toString
        )
          .mapError(e => CacheError(e.getMessage))
        res = if (deleted == 0) false else true
      } yield res
    execCommand[Boolean](config, cmd)
  }

  override def getPostFeed(subscriber: UserId, postFeedLength: Int): Task[List[String]] = {
    val cmd: RedisCommands[Task, String, String] => Task[List[String]] = redis =>
      for {
        feed <- redis.zRange(subscriber.value, - postFeedLength, - 1)
          .mapError(e => CacheError(e.getMessage))
      } yield feed
    execCommand[List[String]](config, cmd)
  }

  override def checkSubscriber(subscriber: UserId): Task[Boolean] = {
    val cmd: RedisCommands[Task, String, String] => Task[Boolean] = redis =>
      redis.exists(subscriber.value).mapError(e => CacheError(e.getMessage))
    execCommand[Boolean](config, cmd)
  }

  override def clearFeed(subscriber: UserId): Task[Boolean] = {
    val cmd: RedisCommands[Task, String, String] => Task[Boolean] = redis =>
      for {
        deleted <- redis.del(subscriber.value)
          .mapError(e => CacheError(e.getMessage))
        res = if (deleted == 0) false else true
      } yield res
    execCommand[Boolean](config, cmd)
  }
}
